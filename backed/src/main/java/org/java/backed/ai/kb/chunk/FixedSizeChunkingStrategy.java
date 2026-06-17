package org.java.backed.ai.kb.chunk;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 固定大小分块策略 — 通用型，适用于 PDF / Word / TXT 等
 * 按段落结构拆分 + 滑动窗口 + 句子边界收尾
 */
@Slf4j
@Component
public class FixedSizeChunkingStrategy implements ChunkingStrategy {

    private static final Pattern PARAGRAPH_SPLIT = Pattern.compile("\\n\\s*\\n");

    @Override
    public String name() { return "FIXED_SIZE"; }

    @Override
    public List<String> supportedMimeTypes() {
        return List.of(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml",
                "text/plain",
                "text/html"
        );
    }

    @Override
    public List<ChunkResult> chunk(String text, ChunkOptions options) {
        if (text == null || text.trim().isEmpty()) return List.of();

        int chunkSize = options.getChunkSize();
        int overlapSize = options.getOverlapSize();
        List<ChunkResult> chunks = new ArrayList<>();
        String[] paragraphs = PARAGRAPH_SPLIT.split(text);
        StringBuilder current = new StringBuilder();
        int idx = 0;

        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) continue;

            if (current.length() > 0 && current.length() + trimmed.length() + 1 > chunkSize) {
                chunks.add(build(current.toString(), idx++));
                current = new StringBuilder(extractOverlap(current.toString(), overlapSize));
            }
            if (current.length() > 0) current.append("\n\n");
            current.append(trimmed);

            while (current.length() >= chunkSize) {
                String slice = current.substring(0, Math.min(current.length(), chunkSize));
                int cut = slice.lastIndexOf('。');
                if (cut > chunkSize / 2) slice = slice.substring(0, cut + 1);
                chunks.add(build(slice, idx++));
                String rest = current.substring(slice.length());
                current = new StringBuilder(extractOverlap(slice, overlapSize) + rest);
            }
        }
        if (current.length() > 0) chunks.add(build(current.toString(), idx++));

        log.debug("FixedSize 分块完成: 块数={}, 原文长度={}", chunks.size(), text.length());
        return chunks;
    }

    private ChunkResult build(String content, int index) {
        return ChunkResult.builder()
                .chunkId(UUID.randomUUID().toString().substring(0, 12))
                .index(index).content(content).build();
    }

    private String extractOverlap(String text, int size) {
        if (text.length() <= size) return text;
        String tail = text.substring(text.length() - size);
        int dot = tail.indexOf('。');
        return (dot > 0 && dot < tail.length() - 1) ? tail.substring(dot + 1) : tail;
    }
}
