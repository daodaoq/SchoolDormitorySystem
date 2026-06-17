package org.java.backed.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宿舍信息实体
 */
@Data
@TableName("dormitory")
public class Dormitory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 宿舍编号（如 A-101），唯一 */
    private String dormitoryNo;

    /** 楼栋 */
    private String building;

    /** 楼层 */
    private String floor;

    /** 房间类型（单人间/双人间/四人间等） */
    private String roomType;

    /** 容纳人数 */
    private Integer capacity;

    /** 当前已入住人数（查询时填充） */
    @TableField(exist = false)
    private Integer occupiedCount;

    /** 状态：ACTIVE / INACTIVE */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
