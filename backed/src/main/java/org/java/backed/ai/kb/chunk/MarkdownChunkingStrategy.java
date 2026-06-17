package org.java.backed.ai.kb.chunk;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 分块策略
 * 按标题层级（# ## ###）切分，代码块保持完整不拆散
 */
@Slf4j
@Component
public class MarkdownChunkingStrategy implements ChunkingStrategy {

    /** 匹配 Markdown 标题行 */
    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);

    /** 匹配代码块 ```...``` */
    private static final Pattern CODE_BLOCK = Pattern.compile("```[\\s\\S]*?```");

    @Override
    public String name() { return "MARKDOWN"; }

    @Override
    public List<String> supportedMimeTypes() {
        return List.of("text/markdown", "text/x-markdown");
    }

    @Override
    public List<ChunkResult> chunk(String text, ChunkOptions options) {
        if (text == null || text.trim().isEmpty()) return List.of();

        // 1. 保护代码块：用占位符替换，防止被拆分
        List<String> codeBlocks = new ArrayList<>();
        String protected_ = CODE_BLOCK.matcher(text).replaceAll(mr -> {
            codeBlocks.add(mr.group());
            return "%%CODEBLOCK_" + (codeBlocks.size() - 1) + "%%";
        });

        // 2. 在标题前插入分隔标记
        String marked = HEADING.matcher(protected_).replaceAll("\n%%SPLIT%%$0");

        // 3. 按分隔标记切分
        String[] sections = marked.split("%%SPLIT%%");
        List<String> rawChunks = new ArrayList<>();

        StringBuilder buf = new StringBuilder();
        for (String sec : sections) {
            String trimmed = sec.trim();
            if (trimmed.isEmpty()) continue;

            // 如果是标题开头，先保存上一个缓冲区
            if (HEADING.matcher(trimmed).lookingAt() && buf.length() > 0) {
                rawChunks.add(buf.toString().trim());
                buf = new StringBuilder();
            }
            if (buf.length() > 0) buf.append("\n\n");
            buf.append(trimmed);
        }
        if (buf.length() > 0) rawChunks.add(buf.toString().trim());

        // 4. 过长的段再按固定大小切分
        int idx = 0;
        List<ChunkResult> results = new ArrayList<>();
        for (String raw : rawChunks) {
            if (raw.length() <= options.getChunkSize()) {
                results.add(build(raw, idx++));
            } else {
                // 对大段按段落二次切分
                for (String sub : splitLarge(raw, options)) {
                    results.add(build(sub, idx++));
                }
            }
        }

        // 5. 还原代码块
        List<ChunkResult> finalResults = new ArrayList<>();
        for (ChunkResult r : results) {
            String content = r.getContent();
            for (int i = 0; i < codeBlocks.size(); i++) {
                content = content.replace("%%CODEBLOCK_" + i + "%%", codeBlocks.get(i));
            }
            finalResults.add(ChunkResult.builder()
                    .chunkId(r.getChunkId()).index(r.getIndex()).content(content).build());
        }

        log.debug("Markdown 分块完成: 标题段={}, 最终块={}", rawChunks.size(), finalResults.size());
        return finalResults;
    }

    private List<String> splitLarge(String text, ChunkOptions options) {
        List<String> parts = new ArrayList<>();
        // 用简单循环，避免正则递归导致的 StackOverflow
        int start = 0;
        int len = text.length();
        int maxSize = options.getChunkSize();
        while (start < len) {
            int end = Math.min(start + maxSize, len);
            // 尝试在句号或换行处截断
            if (end < len) {
                int breakPoint = Math.max(
                        text.lastIndexOf('。', end),
                        text.lastIndexOf('\n', end));
                if (breakPoint > start + maxSize / 3) {
                    end = breakPoint + 1;
                }
            }
            parts.add(text.substring(start, end).trim());
            start = Math.max(start + 1, end - options.getOverlapSize());
        }
        return parts;
    }

    private ChunkResult build(String content, int index) {
        return ChunkResult.builder()
                .chunkId(UUID.randomUUID().toString().substring(0, 12))
                .index(index).content(content).build();
    }
}
