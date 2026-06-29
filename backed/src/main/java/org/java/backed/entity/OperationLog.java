package org.java.backed.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体 — 企业级审计日志
 */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人 ID */
    private Long userId;

    /** 操作人用户名 */
    private String username;

    /** 操作人真实姓名 */
    private String realName;

    /** 操作模块（学生管理 / 账单管理 / 系统管理等） */
    private String module;

    /** 操作类型（新增 / 修改 / 删除 / 登录 / 支付等） */
    private String action;

    /** 操作描述（人可读的摘要，如「修改学生 张三 的信息」） */
    private String description;

    /** 请求方法全限定名 + 方法签名 */
    private String method;

    /** 请求参数（JSON，敏感字段脱敏） */
    private String requestParams;

    /** 执行耗时（毫秒） */
    private Long duration;

    /** 操作结果：SUCCESS / FAIL */
    private String status;

    /** 错误信息（仅失败时记录） */
    private String errorMsg;

    /** 客户端 IP */
    private String ipAddress;

    /** User-Agent */
    private String userAgent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
