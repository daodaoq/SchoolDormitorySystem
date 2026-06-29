package org.java.backed.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 队列/交换机/绑定配置
 */
@Configuration
public class RabbitMQConfig {

    // ========== 交换机 ==========
    public static final String EXCHANGE_BILL = "bill.exchange";
    public static final String EXCHANGE_PAYMENT = "payment.exchange";
    public static final String EXCHANGE_NOTIFICATION = "notification.exchange";
    public static final String EXCHANGE_EXCEL = "excel.exchange";
    public static final String EXCHANGE_KB_DOCUMENT = "kb.document.exchange";
    public static final String EXCHANGE_OPLOG = "oplog.exchange";

    // ========== 队列 ==========
    public static final String QUEUE_BILL_GENERATE = "bill.generate.queue";
    public static final String QUEUE_PAYMENT_CALLBACK = "payment.callback.queue";
    public static final String QUEUE_NOTIFICATION_SEND = "notification.send.queue";
    public static final String QUEUE_EXCEL_EXPORT = "excel.export.queue";
    public static final String QUEUE_KB_DOCUMENT_PROCESS = "kb.document.process.queue";
    public static final String QUEUE_OPLOG = "oplog.queue";

    // ========== 路由键 ==========
    public static final String RK_BILL_GENERATE = "bill.generate";
    public static final String RK_PAYMENT_CALLBACK = "payment.callback";
    public static final String RK_NOTIFICATION_SEND = "notification.send";
    public static final String RK_EXCEL_EXPORT = "excel.export";
    public static final String RK_KB_DOCUMENT_PROCESS = "kb.document.process";
    public static final String RK_OPLOG = "oplog";

    // ========== 交换机定义 ==========
    @Bean
    public DirectExchange billExchange() {
        return new DirectExchange(EXCHANGE_BILL);
    }

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(EXCHANGE_PAYMENT);
    }

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(EXCHANGE_NOTIFICATION);
    }

    @Bean
    public DirectExchange excelExchange() {
        return new DirectExchange(EXCHANGE_EXCEL);
    }

    @Bean
    public DirectExchange kbDocumentExchange() {
        return new DirectExchange(EXCHANGE_KB_DOCUMENT);
    }

    // ========== 队列定义 ==========
    @Bean
    public Queue billGenerateQueue() {
        return QueueBuilder.durable(QUEUE_BILL_GENERATE).build();
    }

    @Bean
    public Queue paymentCallbackQueue() {
        return QueueBuilder.durable(QUEUE_PAYMENT_CALLBACK).build();
    }

    @Bean
    public Queue notificationSendQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION_SEND).build();
    }

    @Bean
    public Queue excelExportQueue() {
        return QueueBuilder.durable(QUEUE_EXCEL_EXPORT).build();
    }

    @Bean
    public Queue kbDocumentProcessQueue() {
        return QueueBuilder.durable(QUEUE_KB_DOCUMENT_PROCESS).build();
    }

    // ========== 绑定关系 ==========
    @Bean
    public Binding bindBillGenerate() {
        return BindingBuilder.bind(billGenerateQueue()).to(billExchange()).with(RK_BILL_GENERATE);
    }

    @Bean
    public Binding bindPaymentCallback() {
        return BindingBuilder.bind(paymentCallbackQueue()).to(paymentExchange()).with(RK_PAYMENT_CALLBACK);
    }

    @Bean
    public Binding bindNotificationSend() {
        return BindingBuilder.bind(notificationSendQueue()).to(notificationExchange()).with(RK_NOTIFICATION_SEND);
    }

    @Bean
    public Binding bindExcelExport() {
        return BindingBuilder.bind(excelExportQueue()).to(excelExchange()).with(RK_EXCEL_EXPORT);
    }

    @Bean
    public Binding bindKbDocumentProcess() {
        return BindingBuilder.bind(kbDocumentProcessQueue()).to(kbDocumentExchange()).with(RK_KB_DOCUMENT_PROCESS);
    }

    // ========== 操作日志 ==========
    @Bean
    public DirectExchange oplogExchange() {
        return new DirectExchange(EXCHANGE_OPLOG);
    }

    @Bean
    public Queue oplogQueue() {
        return QueueBuilder.durable(QUEUE_OPLOG).build();
    }

    @Bean
    public Binding bindOplog() {
        return BindingBuilder.bind(oplogQueue()).to(oplogExchange()).with(RK_OPLOG);
    }

    // ========== 消息序列化 ==========

    /**
     * JSON 消息转换器 — 替代默认的 Java 序列化，避免反序列化安全限制
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate 使用 JSON 序列化
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    /**
     * 操作日志专用监听容器 — 自动 ACK，消费者内部手动 JSON 解析
     * 独立于全局的 manual ACK 配置，避免日志消费因未手动确认而堆积。
     * 不设置 Jackson2JsonMessageConverter，解决旧 Java 序列化消息残留问题。
     */
    @Bean("oplogListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory oplogListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setPrefetchCount(20);
        return factory;
    }
}
