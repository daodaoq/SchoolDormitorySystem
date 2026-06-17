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

    /** 学生照片 URL（MinIO 存储） */
    private String photo;

    /** 关联的系统用户ID（sys_user.id），可为空 */
    private Long userId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
