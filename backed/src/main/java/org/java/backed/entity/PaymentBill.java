package org.java.backed.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 缴费账单实体
 */
@Data
@TableName("payment_bill")
public class PaymentBill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long feeItemId;

    private String billNo;

    private String semester;

    private BigDecimal amount;

    private BigDecimal paidAmount;

    private LocalDate dueDate;

    private String status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ===== 关联查询字段 (非数据库字段) =====
    @TableField(exist = false)
    private String studentName;

    @TableField(exist = false)
    private String studentNo;

    @TableField(exist = false)
    private String dormitoryNo;

    @TableField(exist = false)
    private String feeItemName;

    @TableField(exist = false)
    private String feeType;
}
