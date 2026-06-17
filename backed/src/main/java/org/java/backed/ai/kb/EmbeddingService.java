package org.java.backed.ai.kb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 向量嵌入服务
 * 通过 Spring AI OpenAI Embedding 客户端调用 DeepSeek Embedding API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    /**
     * 对单个文本生成向量
     */
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }

    /**
     * 批量生成向量（每批最多32条）
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        List<float[]> allVectors = new ArrayList<>();
        int batchSize = 32;

        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);

            try {
                List<float[]> batchVectors = embeddingModel.embed(batch);
                allVectors.addAll(batchVectors);
                log.debug("Embedding 批次完成: {}/{}", end, texts.size());
            } catch (Exception e) {
                log.error("Embedding 失败: 批次 {}-{}", i, end, e);
                throw new RuntimeException("向量嵌入失败: " + e.getMessage());
            }
        }

        return allVectors;
    }

    /**
     * 获取向量维度
     */
    public int getDimension() {
        float[] testVec = embed("test");
        return testVec != null ? testVec.length : 1536;
    }
}
