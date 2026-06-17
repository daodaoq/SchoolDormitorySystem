package org.java.backed.ai.kb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 文本分块服务
 * 参照 ragent 的 FixedSizeTextChunker + StructureAwareTextChunker 设计
 */
@Slf4j
@Service
public class TextChunkingService {

    // 默认分块参数
    private static final int DEFAULT_CHUNK_SIZE = 512;    // 每块最多字符数
    private static final int DEFAULT_OVERLAP_SIZE = 128;  // 块间重叠字符数

    /** 段落分隔正则 */
    private static final Pattern PARAGRAPH_SPLIT = Pattern.compile("\\n\\s*\\n");

    /**
     * 智能分块：优先按段落结构切分，段落过长则按固定大小切分
     *
     * @param text 原始文本
     * @return 分块列表
     */
    public List<ChunkResult> chunk(String text) {
        return chunk(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP_SIZE);
    }

    /**
     * 按指定参数分块
     */
    public List<ChunkResult> chunk(String text, int chunkSize, int overlapSize) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }

        List<ChunkResult> chunks = new ArrayList<>();

        // 第一阶段：按段落拆分
        String[] paragraphs = PARAGRAPH_SPLIT.split(text);

        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            // 如果当前块加上新段落超过限制，先保存当前块
            if (currentChunk.length() > 0 &&
                    currentChunk.length() + trimmed.length() + 1 > chunkSize) {
                chunks.add(createChunk(currentChunk.toString(), chunkIndex++));
                // 保留重叠部分
                String overlap = extractOverlap(currentChunk.toString(), overlapSize);
                currentChunk = new StringBuilder(overlap);
            }

            // 添加段落到当前块
            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(trimmed);

            // 如果当前块已足够大，保存并重置
            while (currentChunk.length() >= chunkSize) {
                String chunkText = currentChunk.substring(0,
                        Math.min(currentChunk.length(), chunkSize));
                // 尝试在最后一个句号处截断
                int lastPeriod = chunkText.lastIndexOf('。');
                if (lastPeriod > chunkSize / 2) {
                    chunkText = chunkText.substring(0, lastPeriod + 1);
                }
                chunks.add(createChunk(chunkText, chunkIndex++));
                String remaining = currentChunk.substring(chunkText.length());
                String overlap = extractOverlap(chunkText, overlapSize);
                currentChunk = new StringBuilder(overlap + remaining);
            }
        }

        // 保存最后一个块
        if (currentChunk.length() > 0) {
            chunks.add(createChunk(currentChunk.toString(), chunkIndex++));
        }

        log.info("文本分块完成: 总块数={}, 原始长度={}", chunks.size(), text.length());
        return chunks;
    }

    private ChunkResult createChunk(String content, int index) {
        return ChunkResult.builder()
                .chunkId(UUID.randomUUID().toString().substring(0, 12))
                .index(index)
                .content(content)
                .build();
    }

    /**
     * 从前一个块末尾提取重叠文本
     */
    private String extractOverlap(String text, int overlapSize) {
        if (text.length() <= overlapSize) {
            return text;
        }
        String tail = text.substring(text.length() - overlapSize);
        // 从第一个完整句子开始
        int firstPeriod = tail.indexOf('。');
        if (firstPeriod > 0 && firstPeriod < tail.length() - 1) {
            return tail.substring(firstPeriod + 1);
        }
        return tail;
    }

    /**
     * 分块结果
     */
    @lombok.Data
    @lombok.Builder
    public static class ChunkResult {
        private String chunkId;
        private int index;
        private String content;
    }
}
