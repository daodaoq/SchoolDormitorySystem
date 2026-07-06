package org.java.backed.consumer;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.config.RabbitMQConfig;
import org.java.backed.service.PaymentBillService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 账单生成消息消费者
 */
@Slf4j
@Component
public class BillGenerateConsumer {

    @Autowired
    private PaymentBillService billService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_BILL_GENERATE)
    public void onMessage(Message message, Channel channel) throws IOException {
        // deliveryTag：消息的唯一标识（通道内递增）
        // 用于确认或拒绝特定消息
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            // 从消息读取参数
            String body = new String(message.getBody());
            log.info("收到账单生成消息: {}", body);

            // 简化处理：生成所有收费项目的账单
            billService.generateBills(null, null);

            // basicAck：确认消息已成功处理
            // false：不批量确认（只确认当前消息）
            // 确认后，RabbitMQ 会从队列中移除该消息
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("账单生成消息处理异常", e);

            // basicNack：拒绝消息
            // 参数1：deliveryTag - 消息标识
            // 参数2：false - 不批量拒绝
            // 参数3：true - 重新入队（requeue），让其他消费者重试
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
