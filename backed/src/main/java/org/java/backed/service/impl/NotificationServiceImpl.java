package org.java.backed.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.entity.NotificationRecord;
import org.java.backed.entity.PaymentBill;
import org.java.backed.entity.StudentDormitory;
import org.java.backed.mapper.NotificationRecordMapper;
import org.java.backed.service.NotificationService;
import org.java.backed.service.PaymentBillService;
import org.java.backed.service.StudentDormitoryService;
import org.java.backed.util.EmailUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationRecordMapper, NotificationRecord>
        implements NotificationService {

    @Autowired
    private EmailUtil emailUtil;

    @Autowired
    private StudentDormitoryService studentService;

    @Autowired
    private PaymentBillService billService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final int MAX_RETRY = 3;
    private static final String RATE_LIMIT_KEY = "notify:rate:";

    @Override
    public void sendOverdueNotification(Long studentId, Long billId) {
        StudentDormitory student = studentService.getById(studentId);
        PaymentBill bill = billService.getById(billId);
        if (student == null || bill == null) {
            log.warn("通知参数异常: studentId={}, billId={}", studentId, billId);
            return;
        }

        if (!checkDailyLimit(studentId)) {
            log.info("通知频率超限: studentId={}", studentId);
            return;
        }

        NotificationRecord record = new NotificationRecord();
        record.setStudentId(studentId);
        record.setBillId(billId);
        record.setNotifyType("OVERDUE");
        record.setChannel("EMAIL");
        record.setRecipient(student.getPhone() + "@example.com");
        record.setTitle("逾期缴费提醒");
        record.setContent(String.format("您的账单%s已逾期，请尽快缴费。金额: ¥%s, 截止日期: %s",
                bill.getBillNo(), bill.getAmount(), bill.getDueDate()));
        record.setStatus("PENDING");
        save(record);

        boolean success = sendWithRetry(record);
        if (!success) {
            record.setStatus("FAILED");
            record.setFailReason("邮件发送失败，已重试" + MAX_RETRY + "次");
            updateById(record);
            log.error("逾期通知发送失败: recordId={}, studentId={}", record.getId(), studentId);
        }
    }

    @Override
    public void batchNotifyOverdue() {
        List<PaymentBill> overdueBills = billService.lambdaQuery()
                .eq(PaymentBill::getStatus, "OVERDUE").list();

        for (PaymentBill bill : overdueBills) {
            try {
                sendOverdueNotification(bill.getStudentId(), bill.getId());
            } catch (Exception e) {
                log.error("批量逾期通知异常: billId={}", bill.getId(), e);
            }
        }
        log.info("批量逾期通知完成: count={}", overdueBills.size());
    }

    private boolean sendWithRetry(NotificationRecord record) {
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                record.setRetryCount(i + 1);
                boolean sent = false;
                if ("EMAIL".equals(record.getChannel())) {
                    sent = emailUtil.sendHtmlMail(record.getRecipient(), record.getTitle(), record.getContent());
                }
                if (sent) {
                    record.setStatus("SUCCESS");
                    record.setSendTime(LocalDateTime.now());
                    updateById(record);
                    return true;
                }
            } catch (Exception e) {
                log.warn("通知发送失败(第{}次重试): recordId={}", i + 1, record.getId(), e);
            }
            try { Thread.sleep(2000L * (i + 1)); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        return false;
    }

    private boolean checkDailyLimit(Long studentId) {
        String key = RATE_LIMIT_KEY + studentId;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) stringRedisTemplate.expire(key, 1, TimeUnit.DAYS);
        return count == null || count <= 5;
    }
}
