package org.java.backed.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.java.backed.entity.NotificationRecord;

/**
 * 通知服务接口
 */
public interface NotificationService extends IService<NotificationRecord> {

    void sendOverdueNotification(Long studentId, Long billId);

    void batchNotifyOverdue();
}
