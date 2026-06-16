package org.java.backed.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.config.RabbitMQConfig;
import org.java.backed.service.PaymentService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 支付回调异步处理消费者
 */
@Slf4j
@Component
public class PaymentCallbackConsumer {

    @Autowired
    private PaymentService paymentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_CALLBACK)
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            String body = new String(message.getBody());
            log.info("收到支付回调消息: {}", body);

            @SuppressWarnings("unchecked")
            Map<String, String> params = objectMapper.readValue(body, Map.class);
            String orderNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");

            paymentService.handlePaymentCallback(orderNo, tradeNo);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("支付回调消息处理异常", e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
