package org.java.backed.ai.kb.chunk;

import java.util.List;

/**
 * 文本分块策略接口（策略模式）
 */
public interface ChunkingStrategy {

    /**
     * 策略名称
     */
    String name();

    /**
     * 支持的 MIME 类型前缀（如 "text/markdown", "application/pdf" 等）
     */
    List<String> supportedMimeTypes();

    /**
     * 对文本进行分块
     *
     * @param text    原始文本
     * @param options 分块选项
     * @return 分块结果列表
     */
    List<ChunkResult> chunk(String text, ChunkOptions options);
}
