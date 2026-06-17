package org.java.backed.ai.kb.chunk;

import lombok.Builder;
import lombok.Data;

/**
 * 分块结果
 */
@Data
@Builder
public class ChunkResult {
    private String chunkId;
    private int index;
    private String content;
}
