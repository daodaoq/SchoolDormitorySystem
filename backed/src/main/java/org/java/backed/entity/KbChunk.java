package org.java.backed.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库分块实体（元数据，向量存储在 Milvus）
 */
@Data
@TableName("kb_chunk")
public class KbChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联文档ID */
    private Long documentId;

    /** Milvus chunk_id，用于交叉引用 */
    private String chunkId;

    /** 分块序号(从0开始) */
    private Integer chunkIndex;

    /** 分块文本内容 */
    private String content;

    /** Token数量估算 */
    private Integer tokenCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
