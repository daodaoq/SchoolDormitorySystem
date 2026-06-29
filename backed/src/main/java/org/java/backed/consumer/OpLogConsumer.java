package org.java.backed.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.config.RabbitMQConfig;
import org.java.backed.entity.OperationLog;
import org.java.backed.mapper.OperationLogMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 操作日志消费者 — 从 RabbitMQ 消费日志消息，写入数据库
 * <p>
 * 直接接收原始 Message，手动反序列化 JSON body，
 * 避免 Jackson2JsonMessageConverter 因为旧消息的 Java 序列化 content-type 而拒绝处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpLogConsumer {

    private final OperationLogMapper logMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    @RabbitListener(queues = RabbitMQConfig.QUEUE_OPLOG, containerFactory = "oplogListenerContainerFactory")
    public void handle(Message message) {
        try {
            byte[] body = message.getBody();
            String contentType = message.getMessageProperties().getContentType();

            // 跳过旧的 Java 序列化消息（无法反序列化）
            if (contentType != null && contentType.contains("x-java-serialized-object")) {
                log.debug("跳过旧格式消息（Java序列化），消息体长度: {}", body.length);
                return;
            }

            // JSON 消息 — 手动解析
            Map<String, Object> entry = objectMapper.readValue(body, Map.class);

            OperationLog oplog = new OperationLog();
            oplog.setUserId(toLong(entry.get("userId")));
            oplog.setUsername((String) entry.get("username"));
            oplog.setRealName((String) entry.get("realName"));
            oplog.setModule((String) entry.get("module"));
            oplog.setAction((String) entry.get("action"));
            oplog.setDescription((String) entry.get("description"));
            oplog.setMethod((String) entry.get("method"));
            oplog.setRequestParams((String) entry.get("requestParams"));
            oplog.setDuration(toLong(entry.get("duration")));
            oplog.setStatus((String) entry.get("status"));
            oplog.setErrorMsg((String) entry.get("errorMsg"));
            oplog.setIpAddress((String) entry.get("ipAddress"));
            oplog.setUserAgent((String) entry.get("userAgent"));

            logMapper.insert(oplog);
        } catch (Exception e) {
            log.error("操作日志写入失败", e);
        }
    }

    private Long toLong(Object v) {
        if (v instanceof Long l) return l;
        if (v instanceof Integer i) return i.longValue();
        if (v instanceof Number n) return n.longValue();
        return null;
    }
}
