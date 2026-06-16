package org.java.backed.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI问答日志实体
 */
@Data
@TableName("ai_qa_log")
public class AiQaLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    private String question;

    private String answer;

    private String source;

    private Integer responseTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
