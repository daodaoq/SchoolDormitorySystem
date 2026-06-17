package org.java.backed.ai.kb.chunk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 分块策略工厂 — 根据文件 MIME 类型选择最佳策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChunkingStrategyFactory {

    private final List<ChunkingStrategy> strategies;

    /**
     * 根据文件类型选择分块策略
     *
     * @param fileType 文件类型标识（PDF/WORD/EXCEL/PPT/TXT/MD）
     */
    public ChunkingStrategy selectByFileType(String fileType) {
        if (fileType == null) return defaultStrategy();

        return switch (fileType.toUpperCase()) {
            case "MD", "MARKDOWN" -> byName("MARKDOWN");
            case "EXCEL", "XLS", "XLSX", "CSV" -> byName("TABLE_ROW");
            default -> defaultStrategy();
        };
    }

    /**
     * 根据 MIME 类型选择策略
     */
    public ChunkingStrategy selectByMimeType(String mimeType) {
        if (mimeType == null) return defaultStrategy();

        String lower = mimeType.toLowerCase();
        return strategies.stream()
                .filter(s -> s.supportedMimeTypes().stream().anyMatch(lower::startsWith))
                .findFirst()
                .orElse(defaultStrategy());
    }

    private ChunkingStrategy byName(String name) {
        return strategies.stream()
                .filter(s -> s.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(defaultStrategy());
    }

    private ChunkingStrategy defaultStrategy() {
        return byName("FIXED_SIZE");
    }
}
