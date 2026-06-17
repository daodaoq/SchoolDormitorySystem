package org.java.backed.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档实体
 */
@Data
@TableName("kb_document")
public class KbDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文档标题 */
    private String title;

    /** 文档描述 */
    private String description;

    /** 原始文件名 */
    private String fileName;

    /** 文件类型: PDF/WORD/EXCEL/PPT/TXT/MD */
    private String fileType;

    /** 文件大小(字节) */
    private Long fileSize;

    /** MinIO 文件URL */
    private String fileUrl;

    /** 分块数量 */
    private Integer chunkCount;

    /** 状态: PENDING/PROCESSING/COMPLETED/FAILED */
    private String status;

    /** 失败原因 */
    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
