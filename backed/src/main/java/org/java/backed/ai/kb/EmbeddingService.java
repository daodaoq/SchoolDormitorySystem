package org.java.backed.ai.kb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 向量嵌入服务
 * 直接调用阿里云百炼 DashScope 原生 Embedding API（非 OpenAI 兼容模式）
 */
@Slf4j
@Service
public class EmbeddingService {

    @Value("${bailian.api-key}")
    private String apiKey;

    @Value("${bailian.embedding-model:text-embedding-v2}")
    private String model;

    private final ObjectMapper mapper = new ObjectMapper();
    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding")
            .requestInterceptor((req, body, exec) -> {
                log.debug("Embedding 请求: {} 个文本", body == null ? 0 : 1);
                return exec.execute(req, body);
            })
            .build();

    /** 单次最大文本数（阿里云限制） */
    private static final int MAX_TEXTS_PER_CALL = 25;

    public float[] embed(String text) {
        List<float[]> results = embedBatch(List.of(text));
        return results.isEmpty() ? new float[0] : results.get(0);
    }

    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) return List.of();

        List<float[]> allVectors = new ArrayList<>();

        for (int i = 0; i < texts.size(); i += MAX_TEXTS_PER_CALL) {
            int end = Math.min(i + MAX_TEXTS_PER_CALL, texts.size());
            List<String> batch = texts.subList(i, end);

            try {
                Map<String, Object> body = Map.of(
                        "model", model,
                        "input", Map.of("texts", batch),
                        "parameters", Map.of("text_type", "document")
                );

                String resp = restClient.post()
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + apiKey)
                        .body(body)
                        .retrieve()
                        .body(String.class);

                JsonNode root = mapper.readTree(resp);

                // 检查错误
                if (root.has("code") && !"".equals(root.path("code").asText())) {
                    String code = root.path("code").asText();
                    String msg = root.path("message").asText();
                    log.error("Embedding API 错误: code={}, msg={}", code, msg);
                    throw new RuntimeException("Embedding 失败: " + code + " - " + msg);
                }

                JsonNode embeddings = root.path("output").path("embeddings");
                if (!embeddings.isArray()) {
                    throw new RuntimeException("Embedding 响应缺少 output.embeddings");
                }

                for (JsonNode emb : embeddings) {
                    JsonNode vecNode = emb.path("embedding");
                    float[] vec = new float[vecNode.size()];
                    for (int j = 0; j < vec.length; j++) {
                        vec[j] = vecNode.get(j).floatValue();
                    }
                    allVectors.add(vec);
                }

                log.info("Embedding 批次完成: {}/{}, 维度={}",
                        end, texts.size(), allVectors.isEmpty() ? "?" : allVectors.get(allVectors.size() - 1).length);

            } catch (Exception e) {
                log.error("Embedding 失败: 批次 {}-{}", i, end, e);
                throw new RuntimeException("向量嵌入失败: " + e.getMessage());
            }
        }

        return allVectors;
    }

    public int getDimension() {
        float[] testVec = embed("test");
        return testVec != null ? testVec.length : 1536;
    }
}
