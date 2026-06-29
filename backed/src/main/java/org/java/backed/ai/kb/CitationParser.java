package org.java.backed.ai.kb;

import lombok.extern.slf4j.Slf4j;
import org.java.backed.ai.dto.CitationItem;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 回答引用解析器
 * 从 LLM 回答文本中提取 [N] 格式的引用标记，与 RAG 检索结果交叉匹配，
 * 生成带有置信度和引用状态的结构化 CitationItem 列表
 */
@Slf4j
@Component
public class CitationParser {

    /** 匹配 [1] [2] [1,3] [1,2,3] 等引用标记 */
    private static final Pattern MARKER_PATTERN = Pattern.compile("\\[(\\d+(?:,\\d+)*)\\]");

    /**
     * 解析 LLM 回答中的行内引用标记，与 RAG 检索结果交叉匹配
     *
     * @param llmResponse    LLM 生成的完整回答文本
     * @param ragCitations   RAG 检索到的原始引用列表（按上下文中的编号顺序排列，索引0对应[1]）
     * @return 增强后的引用列表，包含置信度和引用状态
     */
    public List<CitationItem> parse(String llmResponse, List<CitationItem> ragCitations) {
        if (ragCitations == null || ragCitations.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 从回答文本中提取所有被引用的编号
        Set<Integer> referencedIds = new LinkedHashSet<>();
        Matcher matcher = MARKER_PATTERN.matcher(llmResponse != null ? llmResponse : "");
        while (matcher.find()) {
            String group = matcher.group(1);  // e.g. "1" or "1,3" or "1,2,3"
            for (String part : group.split(",")) {
                try {
                    int id = Integer.parseInt(part.trim());
                    if (id > 0) {
                        referencedIds.add(id);
                    }
                } catch (NumberFormatException ignored) {
                    // 非法编号，跳过
                }
            }
        }

        // 2. 交叉匹配：markerId → ragCitations[markerId-1]
        List<CitationItem> result = new ArrayList<>();

        if (referencedIds.isEmpty()) {
            // LLM 没有插入任何引用标记 → 全部标记为 LOW 置信度
            log.debug("LLM 回答中未检测到引用标记，所有引用标记为 LOW");
            for (int i = 0; i < ragCitations.size(); i++) {
                CitationItem raw = ragCitations.get(i);
                result.add(CitationItem.unreferenced(
                        raw.chunkId(), raw.docId(), raw.docTitle(),
                        raw.content(), raw.chunkIndex(), raw.score()));
            }
            return result;
        }

        // 将被引用的标记为 HIGH
        for (Integer markerId : referencedIds) {
            int ragIndex = markerId - 1;
            if (ragIndex >= 0 && ragIndex < ragCitations.size()) {
                CitationItem raw = ragCitations.get(ragIndex);
                result.add(CitationItem.referenced(
                        markerId, raw.chunkId(), raw.docId(), raw.docTitle(),
                        raw.content(), raw.chunkIndex(), raw.score()));
            } else {
                log.debug("LLM 引用编号 [{}] 超出 RAG 结果范围 (总数={})，已忽略",
                        markerId, ragCitations.size());
            }
        }

        log.debug("引用解析完成: 总RAG={}, LLM引用={}, 匹配={}",
                ragCitations.size(), referencedIds.size(), result.size());
        return result;
    }

    /**
     * 将 CitationItem 列表转为 Map 列表（用于 JSON 序列化）
     */
    public static List<Map<String, Object>> toMapList(List<CitationItem> items) {
        if (items == null) return Collections.emptyList();
        List<Map<String, Object>> list = new ArrayList<>();
        for (CitationItem item : items) {
            list.add(item.toMap());
        }
        return list;
    }

    /**
     * 从 Map 列表构建 CitationItem 列表（用于从旧格式转换）
     */
    @SuppressWarnings("unchecked")
    public static List<CitationItem> fromMapList(List<Map<String, Object>> maps) {
        if (maps == null || maps.isEmpty()) return Collections.emptyList();
        List<CitationItem> items = new ArrayList<>();
        for (int i = 0; i < maps.size(); i++) {
            Map<String, Object> m = maps.get(i);
            items.add(new CitationItem(
                    i + 1,  // 默认编号 = 索引 + 1
                    (String) m.getOrDefault("chunkId", ""),
                    toLong(m.get("docId")),
                    (String) m.getOrDefault("docTitle", "未知文档"),
                    (String) m.getOrDefault("content", ""),
                    toInt(m.get("chunkIndex")),
                    toDouble(m.get("score")),
                    "LOW",
                    false
            ));
        }
        return items;
    }

    private static Long toLong(Object v) {
        if (v instanceof Long l) return l;
        if (v instanceof Integer i) return i.longValue();
        if (v instanceof Number n) return n.longValue();
        return 0L;
    }

    private static int toInt(Object v) {
        if (v instanceof Integer i) return i;
        if (v instanceof Long l) return l.intValue();
        if (v instanceof Number n) return n.intValue();
        return 0;
    }

    private static double toDouble(Object v) {
        if (v instanceof Double d) return d;
        if (v instanceof Float f) return f.doubleValue();
        if (v instanceof Number n) return n.doubleValue();
        return 0.0;
    }
}
