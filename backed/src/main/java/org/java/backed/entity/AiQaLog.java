package org.java.backed.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 问答日志实体
 */
@Data
@TableName("ai_qa_log")
public class AiQaLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private String userId;

    /** 用户问题 */
    private String question;

    /** AI 回答内容 */
    private String answer;

    /** 来源：AI / LOCAL_KB / CACHE / FALLBACK / AI_STREAM / RATE_LIMIT / ERROR */
    private String source;

    /** AI 模型名称 */
    private String modelName;

    /** 响应时间（毫秒） */
    private Integer responseTime;

    /** Token 消耗量（估算） */
    private Integer tokenCount;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
