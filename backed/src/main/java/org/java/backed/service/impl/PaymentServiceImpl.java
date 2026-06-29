package org.java.backed.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.common.BusinessException;
import org.java.backed.common.RedisLock;
import org.java.backed.common.SnowflakeIdGenerator;
import org.java.backed.entity.PaymentBill;
import org.java.backed.entity.PaymentRecord;
import org.java.backed.entity.StudentDormitory;
import org.java.backed.mapper.PaymentRecordMapper;
import org.java.backed.service.PaymentBillService;
import org.java.backed.service.PaymentService;
import org.java.backed.service.StudentDormitoryService;
import org.java.backed.util.AlipayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PaymentServiceImpl extends ServiceImpl<PaymentRecordMapper, PaymentRecord> implements PaymentService {

    @Autowired
    private SnowflakeIdGenerator idGenerator;

    @Autowired
    private AlipayUtil alipayUtil;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private PaymentBillService billService;

    @Autowired
    private StudentDormitoryService studentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentRecord createOrder(Long billId) {
        PaymentBill bill = billService.getById(billId);
        if (bill == null) throw new BusinessException(404, "账单不存在");
        if ("PAID".equals(bill.getStatus())) throw new BusinessException(409, "该账单已支付");

        RedisLock lock = new RedisLock(stringRedisTemplate, "payment:create:" + billId);
        try {
            if (!lock.lock(5000)) {
                throw new BusinessException(409, "系统正在处理您的支付请求，请勿重复操作");
            }

            LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PaymentRecord::getBillId, billId);
            wrapper.eq(PaymentRecord::getStatus, "WAITING");
            PaymentRecord existing = getOne(wrapper);
            if (existing != null) {
                // 有未过期的 WAITING 订单，且 receiptUrl 完好 → 直接复用
                if (existing.getReceiptUrl() != null && !existing.getReceiptUrl().isEmpty()
                        && existing.getCreateTime().plusMinutes(15).isAfter(LocalDateTime.now())) {
                    return existing;
                } else {
                    // receiptUrl 为空 或 已过期 → 关闭，重新创建
                    log.info("【支付调试】关闭无效旧订单: orderNo={}, receiptUrl为空={}, 已过期={}",
                            existing.getOrderNo(),
                            existing.getReceiptUrl() == null || existing.getReceiptUrl().isEmpty(),
                            !existing.getCreateTime().plusMinutes(15).isAfter(LocalDateTime.now()));
                    existing.setStatus("CLOSED");
                    updateById(existing);
                }
            }

            String orderNo = idGenerator.generateOrderNo();
            PaymentRecord record = new PaymentRecord();
            record.setBillId(billId);
            record.setStudentId(bill.getStudentId());
            record.setOrderNo(orderNo);
            record.setAmount(bill.getAmount().subtract(bill.getPaidAmount()));
            record.setPayMethod("ALIPAY");
            record.setStatus("WAITING");

            StudentDormitory student = studentService.getById(bill.getStudentId());
            String subject = "宿舍费用缴费 - " + (student != null ? student.getDormitoryNo() : "");

            String payForm = alipayUtil.createPayPage(orderNo, subject,
                    "账单编号: " + bill.getBillNo(), record.getAmount().toString());
            log.info("【支付调试】createPayPage 返回: isNull={}, length={}, preview={}",
                    payForm == null, payForm != null ? payForm.length() : 0,
                    payForm != null ? payForm.substring(0, Math.min(200, payForm.length())) : "NULL");

            if (payForm != null) {
                record.setReceiptUrl(payForm);
                log.info("【支付调试】save 前 receiptUrl length={}", record.getReceiptUrl().length());
                save(record);
                // 立即验证是否写入
                PaymentRecord verify = getById(record.getId());
                log.info("【支付调试】save 后验证 receiptUrl: isNull={}, length={}",
                        verify != null && verify.getReceiptUrl() != null,
                        verify != null && verify.getReceiptUrl() != null ? verify.getReceiptUrl().length() : 0);
                return record;
            } else {
                throw new BusinessException(500, "创建支付订单失败，请稍后重试");
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentCallback(String orderNo, String tradeNo) {
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentRecord::getOrderNo, orderNo);
        PaymentRecord record = getOne(wrapper);
        if (record == null) {
            log.warn("支付记录不存在: orderNo={}", orderNo);
            return;
        }
        if ("SUCCESS".equals(record.getStatus())) {
            log.info("支付记录已处理(幂等): orderNo={}", orderNo);
            return;
        }

        record.setStatus("SUCCESS");
        record.setTradeNo(tradeNo);
        record.setPayTime(LocalDateTime.now());
        updateById(record);

        PaymentBill bill = billService.getById(record.getBillId());
        if (bill != null) {
            bill.setPaidAmount(bill.getPaidAmount().add(record.getAmount()));
            if (bill.getPaidAmount().compareTo(bill.getAmount()) >= 0) {
                bill.setStatus("PAID");
            }
            billService.updateById(bill);

            StudentDormitory student = studentService.getById(bill.getStudentId());
            if (student != null) {
                LambdaQueryWrapper<PaymentBill> billWrapper = new LambdaQueryWrapper<>();
                billWrapper.eq(PaymentBill::getStudentId, student.getId());
                billWrapper.ne(PaymentBill::getStatus, "PAID");
                if (billService.count(billWrapper) == 0) {
                    student.setPaymentStatus("PAID");
                    studentService.updateById(student);
                }
            }
        }
        log.info("支付回调处理完成: orderNo={}, tradeNo={}", orderNo, tradeNo);
    }

    @Override
    public Page<PaymentRecord> queryRecords(int pageNum, int pageSize, String studentNo) {
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        if (studentNo != null && !studentNo.isEmpty()) {
            LambdaQueryWrapper<StudentDormitory> stuWrapper = new LambdaQueryWrapper<>();
            stuWrapper.like(StudentDormitory::getStudentNo, studentNo);
            StudentDormitory student = studentService.getOne(stuWrapper);
            if (student != null) wrapper.eq(PaymentRecord::getStudentId, student.getId());
        }
        wrapper.orderByDesc(PaymentRecord::getCreateTime);
        Page<PaymentRecord> page = page(new Page<>(pageNum, pageSize), wrapper);
        fillPaymentDetails(page.getRecords());
        return page;
    }

    @Override
    public void closeTimeoutOrders() {
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentRecord::getStatus, "WAITING");
        wrapper.le(PaymentRecord::getCreateTime, LocalDateTime.now().minusMinutes(15));
        List<PaymentRecord> timeoutOrders = list(wrapper);
        for (PaymentRecord record : timeoutOrders) {
            record.setStatus("CLOSED");
            updateById(record);
        }
        if (!timeoutOrders.isEmpty()) {
            log.info("关闭超时订单: count={}", timeoutOrders.size());
        }
    }

    private void fillPaymentDetails(List<PaymentRecord> records) {
        if (records.isEmpty()) return;
        Map<Long, StudentDormitory> studentMap = studentService.list().stream()
                .collect(Collectors.toMap(StudentDormitory::getId, s -> s));
        Map<Long, PaymentBill> billMap = billService.list().stream()
                .collect(Collectors.toMap(PaymentBill::getId, b -> b));

        for (PaymentRecord record : records) {
            StudentDormitory student = studentMap.get(record.getStudentId());
            if (student != null) {
                record.setStudentName(student.getStudentName());
                record.setStudentNo(student.getStudentNo());
            }
            PaymentBill bill = billMap.get(record.getBillId());
            if (bill != null) {
                record.setBillNo(bill.getBillNo());
            }
        }
    }
}
