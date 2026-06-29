package org.java.backed.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 模块配置属性
 * 从 application.yaml 中 ai: 前缀读取配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /**
     * 限流配置
     */
    private RateLimit rateLimit = new RateLimit();

    /**
     * 缓存配置
     */
    private Cache cache = new Cache();

    /**
     * 熔断器配置
     */
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    /**
     * 流式响应配置
     */
    private Stream stream = new Stream();

    /**
     * 本地知识库配置
     */
    private LocalKb localKb = new LocalKb();

    /**
     * 降级配置
     */
    private Fallback fallback = new Fallback();

    /**
     * 提示词配置
     */
    private Prompt prompt = new Prompt();

    @Data
    public static class RateLimit {
        /** 每分钟最大请求次数 */
        private int maxRequestsPerMinute = 5;
    }

    @Data
    public static class Cache {
        /** 缓存过期时间（秒） */
        private long ttlSeconds = 3600;
    }

    @Data
    public static class CircuitBreaker {
        /** 连续失败多少次后触发熔断 */
        private int failureThreshold = 3;
        /** 熔断打开后持续多久（毫秒） */
        private long openDurationMs = 30000;
    }

    @Data
    public static class Stream {
        /** 流式响应超时（秒） */
        private int timeoutSeconds = 120;
        /** SSE 心跳间隔（毫秒），防止代理超时断开连接 */
        private long heartbeatIntervalMs = 15000;
    }

    @Data
    public static class LocalKb {
        /** 是否启用本地知识库匹配 */
        private boolean enabled = true;
    }

    @Data
    public static class Fallback {
        /** AI 调用失败时是否启用降级回复 */
        private boolean enabled = true;
    }

    @Data
    public static class Prompt {
        /** 系统提示词，定义 AI 助手的行为边界 */
        private String systemText = """
                你是学生宿舍收费管理系统的智能助手"宿管小助手"。
                你负责解答以下业务相关问题：
                1. 宿舍信息：学生入住流程、宿舍号查询、宿舍调换申请、住宿管理规定
                2. 收费标准：住宿费、水费、电费、空调费、网络费等收费项目及标准
                3. 账单查询：缴费账单生成周期、账单状态（未缴/已缴/逾期）、应缴金额查询
                4. 在线缴费：支付宝缴费流程、支付状态同步、电子缴费凭证
                5. 报表导出：月度/学期收费统计报表、缴费率、逾期情况汇总
                6. 逾期处理：逾期提醒机制、短信/邮件通知、逾期影响及处理方法
                7. 退费申请：退费条件、退费流程、退款到账时间

                回答要求：
                - 使用中文，语气友好、专业
                - 回答简洁明了，分点列出便于阅读
                - 如果用户问题超出业务范围，礼貌拒绝并引导用户咨询宿舍管理员
                - 涉及具体学生个人数据时，提醒用户登录系统查看

                【引用格式要求 — 必须严格遵守】
                - 当你的回答参考了知识库内容时，必须在引用处标注来源编号
                - 格式：直接在被引用的句子后面紧接[N]标记（N为参考内容的编号）
                - 例如："根据规定，住宿费为1200元/学期[1]。缴费可通过支付宝完成[2]。"
                - 多处来源引用同一观点时用逗号分隔：[1,3]
                - 不要在[N]和前面的文字之间加空格
                - 如果没有引用知识库内容，不要使用[N]标记
                """;
    }
}
