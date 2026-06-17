package org.java.backed.ai.kb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * 重排序服务
 * 向量检索粗筛后，调用阿里云百炼 Rerank API 精排
 * 参照 ragent 的 BaiLianRerankClient 设计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RerankService {

    @Value("${bailian.api-key}")
    private String apiKey;

    private final ObjectMapper mapper = new ObjectMapper();
    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank")
            .build();

    /** Rerank 后保留的 Top-N */
    private static final int RERANK_TOP_N = 3;

    /**
     * 对向量检索结果进行重排序
     *
     * @param query   用户查询
     * @param results 向量检索 Top-K 结果（通常 10-20 条）
     * @return 重排序后的 Top-N 结果
     */
    public List<Map<String, Object>> rerank(String query, List<Map<String, Object>> results) {
        if (results == null || results.size() <= RERANK_TOP_N) {
            return results; // 不够多，没必要重排
        }

        try {
            List<String> documents = results.stream()
                    .map(r -> (String) r.get("content"))
                    .toList();

            Map<String, Object> body = Map.of(
                    "model", "gte-rerank",
                    "query", query,
                    "documents", documents,
                    "top_n", RERANK_TOP_N,
                    "return_documents", false
            );

            String resp = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = mapper.readTree(resp);
            JsonNode resultsNode = root.path("output").path("results");
            if (!resultsNode.isArray()) {
                log.warn("Rerank 返回格式异常，使用原始排序");
                return results.subList(0, RERANK_TOP_N);
            }

            // 按 rerank 结果重建顺序
            List<Map<String, Object>> reranked = new ArrayList<>();
            for (JsonNode node : resultsNode) {
                int idx = node.path("index").asInt();
                double score = node.path("relevance_score").asDouble();
                if (idx >= 0 && idx < results.size()) {
                    Map<String, Object> item = new LinkedHashMap<>(results.get(idx));
                    item.put("score", Math.round(score * 10000.0) / 10000.0);
                    item.put("reranked", true);
                    reranked.add(item);
                }
            }

            log.info("Rerank 完成: {} 条 → {} 条, query={}",
                    results.size(), reranked.size(),
                    query.length() > 30 ? query.substring(0, 30) + "..." : query);
            return reranked;

        } catch (Exception e) {
            log.warn("Rerank 失败，使用原始排序（不影响主流程）: {}", e.getMessage());
            return results.subList(0, Math.min(RERANK_TOP_N, results.size()));
        }
    }
}
