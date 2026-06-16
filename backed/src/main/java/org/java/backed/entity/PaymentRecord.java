package org.java.backed.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水实体
 */
@Data
@TableName("payment_record")
public class PaymentRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long billId;

    private Long studentId;

    private String orderNo;

    private BigDecimal amount;

    private String payMethod;

    private String tradeNo;

    private LocalDateTime payTime;

    private String status;

    private String receiptUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ===== 关联查询字段 =====
    @TableField(exist = false)
    private String studentName;

    @TableField(exist = false)
    private String studentNo;

    @TableField(exist = false)
    private String billNo;

    @TableField(exist = false)
    private String feeItemName;
}
