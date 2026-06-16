package org.java.backed.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知记录实体
 */
@Data
@TableName("notification_record")
public class NotificationRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long billId;

    private String notifyType;

    private String channel;

    private String recipient;

    private String title;

    private String content;

    private String status;

    private Integer retryCount;

    private LocalDateTime sendTime;

    private String failReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
