package org.java.backed.ai.dto;

/**
 * AI 引用来源项（用于溯源）
 * 替代原有的 Map<String, Object> 方式，提供结构化引用数据
 */
public record CitationItem(
    /** 引用标记编号，对应回答中的 [N] */
    int markerId,
    /** Milvus 中的 chunk_id */
    String chunkId,
    /** 知识库文档 ID */
    Long docId,
    /** 文档标题 */
    String docTitle,
    /** 分块原文内容 */
    String content,
    /** 段落序号（从 0 开始） */
    int chunkIndex,
    /** 相关度分值 */
    double score,
    /** 置信度：HIGH（被 LLM 实际引用）/ LOW（仅检索未引用） */
    String confidence,
    /** LLM 是否在回答中实际引用了此来源 */
    boolean referenced
) {
    /** 创建未被引用的 CitationItem（confidence=LOW） */
    public static CitationItem unreferenced(String chunkId, Long docId, String docTitle,
                                            String content, int chunkIndex, double score) {
        return new CitationItem(0, chunkId, docId, docTitle, content, chunkIndex, score, "LOW", false);
    }

    /** 创建被引用的 CitationItem（confidence=HIGH） */
    public static CitationItem referenced(int markerId, String chunkId, Long docId, String docTitle,
                                          String content, int chunkIndex, double score) {
        return new CitationItem(markerId, chunkId, docId, docTitle, content, chunkIndex, score, "HIGH", true);
    }

    /** 转为 Map 用于 JSON 序列化（向后兼容） */
    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("markerId", markerId);
        map.put("chunkId", chunkId);
        map.put("docId", docId);
        map.put("docTitle", docTitle);
        map.put("content", content);
        map.put("chunkIndex", chunkIndex);
        map.put("score", score);
        map.put("confidence", confidence);
        map.put("referenced", referenced);
        return map;
    }
}
