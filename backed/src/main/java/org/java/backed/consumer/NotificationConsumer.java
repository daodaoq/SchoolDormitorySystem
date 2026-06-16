package org.java.backed.consumer;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.config.RabbitMQConfig;
import org.java.backed.service.NotificationService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 通知发送消费者
 */
@Slf4j
@Component
public class NotificationConsumer {

    @Autowired
    private NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION_SEND)
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            String body = new String(message.getBody());
            log.info("收到通知发送消息: {}", body);

            // 解析消息: studentId:billId
            String[] parts = body.split(":");
            if (parts.length == 2) {
                Long studentId = Long.parseLong(parts[0]);
                Long billId = Long.parseLong(parts[1]);
                notificationService.sendOverdueNotification(studentId, billId);
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("通知发送消息处理异常", e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
