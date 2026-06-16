package org.java.backed.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收费项目实体
 */
@Data
@TableName("fee_item")
public class FeeItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String itemName;

    private String feeType;

    private BigDecimal unitPrice;

    private String billingCycle;

    private String applicableDormType;

    private String status;

    private String description;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
