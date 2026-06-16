package org.java.backed.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String operator;

    private String module;

    private String action;

    private Long targetId;

    private String detail;

    private String ipAddress;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
