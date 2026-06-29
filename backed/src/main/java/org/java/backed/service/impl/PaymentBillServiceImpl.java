package org.java.backed.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.common.BusinessException;
import org.java.backed.common.SnowflakeIdGenerator;
import org.java.backed.entity.FeeItem;
import org.java.backed.entity.PaymentBill;
import org.java.backed.entity.StudentDormitory;
import org.java.backed.mapper.PaymentBillMapper;
import org.java.backed.service.FeeItemService;
import org.java.backed.service.PaymentBillService;
import org.java.backed.service.StudentDormitoryService;
import org.java.backed.util.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PaymentBillServiceImpl extends ServiceImpl<PaymentBillMapper, PaymentBill> implements PaymentBillService {

    @Autowired
    private SnowflakeIdGenerator idGenerator;

    @Autowired
    private StudentDormitoryService studentService;

    @Autowired
    private FeeItemService feeItemService;

    @Override
    public Page<PaymentBill> queryPage(int pageNum, int pageSize, String studentNo,
                                        String dormitoryNo, String semester, String status, String feeType) {
        LambdaQueryWrapper<StudentDormitory> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.like(studentNo != null && !studentNo.isEmpty(), StudentDormitory::getStudentNo, studentNo);
        studentWrapper.like(dormitoryNo != null && !dormitoryNo.isEmpty(), StudentDormitory::getDormitoryNo, dormitoryNo);
        List<Long> studentIds = studentService.list(studentWrapper).stream().map(StudentDormitory::getId).toList();

        if ((studentNo != null && !studentNo.isEmpty() || dormitoryNo != null && !dormitoryNo.isEmpty()) && studentIds.isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }

        LambdaQueryWrapper<PaymentBill> wrapper = new LambdaQueryWrapper<>();
        if (!studentIds.isEmpty()) {
            wrapper.in(PaymentBill::getStudentId, studentIds);
        }
        wrapper.eq(semester != null && !semester.isEmpty(), PaymentBill::getSemester, semester);
        if (status != null && !status.isEmpty()) {
            String[] statuses = status.split(",");
            wrapper.in(PaymentBill::getStatus, Arrays.asList(statuses));
        }
        wrapper.orderByDesc(PaymentBill::getCreateTime);

        Page<PaymentBill> page = page(new Page<>(pageNum, pageSize), wrapper);
        fillBillDetails(page.getRecords());
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateBills(String semester, List<Long> feeItemIds) {
        List<FeeItem> feeItems;
        if (feeItemIds != null && !feeItemIds.isEmpty()) {
            feeItems = feeItemService.listByIds(feeItemIds);
        } else {
            feeItems = feeItemService.getActiveFeeItems();
        }

        if (feeItems.isEmpty()) {
            throw new BusinessException(400, "没有可用的收费项目");
        }

        List<StudentDormitory> students = studentService.list();
        int count = 0;

        for (StudentDormitory student : students) {
            for (FeeItem feeItem : feeItems) {
                LambdaQueryWrapper<PaymentBill> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(PaymentBill::getStudentId, student.getId());
                wrapper.eq(PaymentBill::getFeeItemId, feeItem.getId());
                wrapper.eq(PaymentBill::getSemester, semester);
                if (count(wrapper) > 0) continue;

                PaymentBill bill = new PaymentBill();
                bill.setStudentId(student.getId());
                bill.setFeeItemId(feeItem.getId());
                bill.setBillNo(idGenerator.generateBillNo());
                bill.setSemester(semester);
                bill.setAmount(feeItem.getUnitPrice());
                bill.setPaidAmount(BigDecimal.ZERO);
                bill.setDueDate(LocalDate.now().plusMonths(1));
                bill.setStatus("UNPAID");
                save(bill);
                count++;
            }
        }
        log.info("账单批量生成完成: semester={}, count={}", semester, count);
        return count;
    }

    @Override
    public boolean updateBillStatus(Long billId, String status, String remark) {
        PaymentBill bill = getById(billId);
        if (bill == null) {
            throw new BusinessException(404, "账单不存在");
        }
        bill.setStatus(status);
        bill.setRemark(remark);
        return updateById(bill);
    }

    @Override
    public byte[] exportBills(String studentNo, String dormitoryNo, String semester, String status) throws IOException {
        Page<PaymentBill> page = queryPage(1, 10000, studentNo, dormitoryNo, semester, status, null);
        return ExcelUtil.exportBills(page.getRecords());
    }

    private void fillBillDetails(List<PaymentBill> bills) {
        if (bills.isEmpty()) return;
        Map<Long, StudentDormitory> studentMap = studentService.list().stream()
                .collect(Collectors.toMap(StudentDormitory::getId, s -> s));
        Map<Long, FeeItem> feeItemMap = feeItemService.list().stream()
                .collect(Collectors.toMap(FeeItem::getId, f -> f));

        for (PaymentBill bill : bills) {
            StudentDormitory student = studentMap.get(bill.getStudentId());
            if (student != null) {
                bill.setStudentName(student.getStudentName());
                bill.setStudentNo(student.getStudentNo());
                bill.setDormitoryNo(student.getDormitoryNo());
            }
            FeeItem feeItem = feeItemMap.get(bill.getFeeItemId());
            if (feeItem != null) {
                bill.setFeeItemName(feeItem.getItemName());
                bill.setFeeType(feeItem.getFeeType());
            }
        }
    }
}
