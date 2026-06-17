package org.java.backed.consumer;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.ai.kb.DocumentProcessingPipeline;
import org.java.backed.config.RabbitMQConfig;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 知识库文档处理消息消费者
 * 上传文档后由 MQ 异步触发：解析 → 分块 → 向量化 → 存储
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KbDocumentProcessConsumer {

    private final DocumentProcessingPipeline pipeline;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_KB_DOCUMENT_PROCESS)
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody());
        log.info("收到知识库文档处理消息: {}", body);

        try {
            Long documentId = Long.parseLong(body.trim());
            pipeline.process(documentId);
            channel.basicAck(deliveryTag, false);
            log.info("知识库文档处理完成: docId={}", documentId);
        } catch (Exception e) {
            log.error("知识库文档处理失败: body={}", body, e);
            // 不重试，避免死循环；失败状态已由 pipeline 写入数据库
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
