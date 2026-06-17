package org.java.backed.ai.kb;

import lombok.extern.slf4j.Slf4j;
import org.java.backed.ai.kb.chunk.ChunkResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 分块质量评估器
 * 在向量化之前过滤低质量 chunk，节省 Embedding API 调用
 */
@Slf4j
@Component
public class ChunkQualityEvaluator {

    /** 纯数字/符号/空白组成的块，无实际语义 */
    private static final Pattern NUMERIC_ONLY = Pattern.compile("^[\\d\\s.,;:!?()（）\\[\\]【】{}、。，；：！？…—\"]+$");

    /** URL 为主的块 */
    private static final Pattern URL_HEAVY = Pattern.compile("^https?://.*$", Pattern.MULTILINE);

    /** 页面页眉页脚噪音 */
    private static final Pattern HEADER_FOOTER = Pattern.compile("^(第\\s*\\d+\\s*页|Page\\s+\\d+|目录|Table of Contents|版权所有|Copyright)\\s*$");

    /**
     * 评估并过滤低质量 Chunk
     *
     * @param chunks 原始分块列表
     * @return 过滤后的高质量分块列表
     */
    public List<ChunkResult> evaluate(List<ChunkResult> chunks) {
        if (chunks == null || chunks.isEmpty()) return List.of();

        List<ChunkResult> good = new ArrayList<>();
        List<String> removedReasons = new ArrayList<>();

        for (ChunkResult c : chunks) {
            String text = c.getContent() == null ? "" : c.getContent().trim();
            String reason = null;

            if (text.length() < 20) {
                reason = "过短(<20字符)";
            } else if (NUMERIC_ONLY.matcher(text).matches()) {
                reason = "纯数字/符号";
            } else if (text.length() > 100 && countUrlLines(text) > text.split("\\n").length * 0.7) {
                reason = "URL 占比过高";
            } else if (HEADER_FOOTER.matcher(text).matches()) {
                reason = "页眉页脚噪音";
            }

            if (reason != null) {
                removedReasons.add(reason);
            } else {
                good.add(c);
            }
        }

        if (!removedReasons.isEmpty()) {
            log.info("Chunk 质量评估: {} 条 → {} 条, 移除原因: {}",
                    chunks.size(), good.size(),
                    removedReasons.stream()
                            .collect(Collectors.groupingBy(r -> r, Collectors.counting())));
        }

        // 重新编号
        for (int i = 0; i < good.size(); i++) {
            good.get(i).setIndex(i);
        }

        return good;
    }

    private long countUrlLines(String text) {
        return text.lines().filter(l -> l.trim().matches("^https?://.*")).count();
    }
}
