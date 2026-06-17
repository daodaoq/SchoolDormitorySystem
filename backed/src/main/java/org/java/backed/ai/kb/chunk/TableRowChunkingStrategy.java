package org.java.backed.ai.kb.chunk;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 表格行分块策略 — 适用于 Excel / CSV 等表格类文件
 * 保持行的完整性，不会把一行数据拆到两个块里
 */
@Slf4j
@Component
public class TableRowChunkingStrategy implements ChunkingStrategy {

    /** 匹配表格分隔行（Markdown 表格的 |---|---|---| 行） */
    private static final Pattern SEPARATOR_ROW = Pattern.compile("^\\|[-: |]+\\|$");

    @Override
    public String name() { return "TABLE_ROW"; }

    @Override
    public List<String> supportedMimeTypes() {
        return List.of(
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml",
                "text/csv",
                "text/tab-separated-values"
        );
    }

    @Override
    public List<ChunkResult> chunk(String text, ChunkOptions options) {
        if (text == null || text.trim().isEmpty()) return List.of();

        // 1. 检测表格类型：Markdown 表格 vs CSV/TSV
        if (text.contains("|") && text.contains("---")) {
            return chunkMarkdownTable(text, options);
        }
        return chunkCsvLike(text, options);
    }

    /**
     * Markdown 表格分块：以表头+分隔行+数据行为单位
     */
    private List<ChunkResult> chunkMarkdownTable(String text, ChunkOptions options) {
        String[] lines = text.split("\\n");
        List<String> header = new ArrayList<>();
        List<String> dataRows = new ArrayList<>();

        boolean headerDone = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (!headerDone && trimmed.startsWith("|")) {
                if (SEPARATOR_ROW.matcher(trimmed).matches()) { headerDone = true; continue; }
                header.add(trimmed);
            } else if (headerDone && trimmed.startsWith("|")) {
                dataRows.add(trimmed);
            }
        }

        String headerBlock = String.join("\n", header);
        int idx = 0;
        List<ChunkResult> results = new ArrayList<>();
        StringBuilder cur = new StringBuilder(headerBlock);

        for (String row : dataRows) {
            if (cur.length() + row.length() > options.getChunkSize() && cur.length() > headerBlock.length()) {
                results.add(build(cur.toString(), idx++));
                cur = new StringBuilder(headerBlock); // 每块都带表头
            }
            cur.append("\n").append(row);
        }
        if (cur.length() > headerBlock.length()) results.add(build(cur.toString(), idx++));

        log.debug("TableRow(MD) 分块完成: {} 行 → {} 块", dataRows.size(), results.size());
        return results;
    }

    /**
     * CSV / TSV 分块：表头行 + 数据行，行完整不拆
     */
    private List<ChunkResult> chunkCsvLike(String text, ChunkOptions options) {
        String[] lines = text.split("\\n");
        if (lines.length == 0) return List.of();

        String headerLine = lines[0]; // 第一行是表头
        int idx = 0;
        List<ChunkResult> results = new ArrayList<>();
        StringBuilder cur = new StringBuilder(headerLine);

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            if (cur.length() + line.length() > options.getChunkSize() && cur.length() > headerLine.length()) {
                results.add(build(cur.toString(), idx++));
                cur = new StringBuilder(headerLine);
            }
            cur.append("\n").append(line);
        }
        if (cur.length() > headerLine.length()) results.add(build(cur.toString(), idx++));

        log.debug("TableRow(CSV) 分块完成: {} 行 → {} 块", lines.length - 1, results.size());
        return results;
    }

    private ChunkResult build(String content, int index) {
        return ChunkResult.builder()
                .chunkId(UUID.randomUUID().toString().substring(0, 12))
                .index(index).content(content).build();
    }
}
