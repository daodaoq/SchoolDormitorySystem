package org.java.backed.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学生宿舍信息实体
 */
@Data
@TableName("student_dormitory")
public class StudentDormitory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String studentName;

    private String studentNo;

    private String dormitoryNo;

    private String phone;

    private LocalDate checkInDate;

    private String paymentStatus;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
