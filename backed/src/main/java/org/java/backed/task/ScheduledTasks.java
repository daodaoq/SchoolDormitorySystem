package org.java.backed.task;

import lombok.extern.slf4j.Slf4j;
import org.java.backed.entity.PaymentBill;
import org.java.backed.entity.PaymentRecord;
import org.java.backed.service.NotificationService;
import org.java.backed.service.PaymentBillService;
import org.java.backed.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统定时任务
 */
@Slf4j
@Component
public class ScheduledTasks {

    @Autowired
    private PaymentBillService billService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private NotificationService notificationService;

    /**
     * 逾期账单检查 - 每天8:00
     */
    @Scheduled(cron = "${schedule.overdue-check}")
    public void checkOverdueBills() {
        log.info("开始检查逾期账单...");
        List<PaymentBill> unpaidBills = billService.lambdaQuery()
                .in(PaymentBill::getStatus, "UNPAID")
                .lt(PaymentBill::getDueDate, LocalDate.now())
                .list();

        int count = 0;
        for (PaymentBill bill : unpaidBills) {
            bill.setStatus("OVERDUE");
            billService.updateById(bill);
            count++;
        }
        log.info("逾期账单检查完成: 标记逾期{}条", count);

        // 发送逾期通知
        if (count > 0) {
            notificationService.batchNotifyOverdue();
        }
    }

    /**
     * 支付订单超时关闭 - 每5分钟
     */
    @Scheduled(cron = "${schedule.order-timeout}")
    public void closeTimeoutOrders() {
        log.debug("检查超时支付订单...");
        paymentService.closeTimeoutOrders();
    }

    /**
     * 统计预计算 - 每天凌晨2:00
     */
    @Scheduled(cron = "${schedule.stats-precompute}")
    public void precomputeStats() {
        log.info("开始统计预计算...");
        // 统计预计算逻辑（将结果缓存到Redis）
        // 此处在实际部署时可将统计结果写入Redis缓存
        log.info("统计预计算完成");
    }
}
