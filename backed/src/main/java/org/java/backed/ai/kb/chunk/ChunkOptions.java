package org.java.backed.ai.kb.chunk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分块配置参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkOptions {

    /** 每块最大字符数 */
    @Builder.Default
    private int chunkSize = 512;

    /** 块间重叠字符数 */
    @Builder.Default
    private int overlapSize = 128;
}
