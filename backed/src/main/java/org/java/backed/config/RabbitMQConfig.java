package org.java.backed.config;

import org.springframework.amqp.core.*;
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

    // ========== 队列 ==========
    public static final String QUEUE_BILL_GENERATE = "bill.generate.queue";
    public static final String QUEUE_PAYMENT_CALLBACK = "payment.callback.queue";
    public static final String QUEUE_NOTIFICATION_SEND = "notification.send.queue";
    public static final String QUEUE_EXCEL_EXPORT = "excel.export.queue";
    public static final String QUEUE_KB_DOCUMENT_PROCESS = "kb.document.process.queue";

    // ========== 路由键 ==========
    public static final String RK_BILL_GENERATE = "bill.generate";
    public static final String RK_PAYMENT_CALLBACK = "payment.callback";
    public static final String RK_NOTIFICATION_SEND = "notification.send";
    public static final String RK_EXCEL_EXPORT = "excel.export";
    public static final String RK_KB_DOCUMENT_PROCESS = "kb.document.process";

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
}
