package org.java.backed.ai.kb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 语义缓存服务 — 基于 Embedding 相似度匹配历史问答
 * 替代原来简单的 hash 缓存，语义相近的问题也能命中
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticCacheService {

    private final StringRedisTemplate redis;
    private final EmbeddingService embeddingService;

    private static final String KEY_PREFIX = "ai:semcache:";
    private static final double SIMILARITY_THRESHOLD = 0.92;
    private static final int MAX_CACHED = 100;
    private static final long TTL_HOURS = 24;

    /**
     * 查找语义相似的历史回答
     *
     * @param question 用户问题
     * @return 缓存的回答（含完整结果），无匹配返回 null
     */
    @SuppressWarnings("unchecked")
    public CachedAnswer lookup(String question) {
        try {
            float[] queryVec = embeddingService.embed(question);

            // 扫描所有缓存 key（数量控制在 MAX_CACHED 以内，性能可接受）
            var keys = redis.keys(KEY_PREFIX + "*");
            if (keys == null || keys.isEmpty()) return null;

            String bestKey = null;
            double bestScore = 0;

            for (String key : keys) {
                String vecStr = redis.opsForValue().get(key + ":vec");
                if (vecStr == null) continue;
                byte[] vecBytes;
                try { vecBytes = Base64.getDecoder().decode(vecStr); }
                catch (IllegalArgumentException e) { continue; } // 旧格式跳过
                if (vecBytes.length == 0) continue;

                float[] cachedVec = bytesToFloats(vecBytes);
                double similarity = cosineSimilarity(queryVec, cachedVec);

                if (similarity > bestScore) {
                    bestScore = similarity;
                    bestKey = key;
                }
            }

            if (bestKey != null && bestScore >= SIMILARITY_THRESHOLD) {
                String answer = redis.opsForValue().get(bestKey + ":ans");
                String source = redis.opsForValue().get(bestKey + ":src");
                if (answer != null) {
                    log.info("语义缓存命中: score={}, question={}", Math.round(bestScore * 10000.0) / 10000.0,
                            question.length() > 30 ? question.substring(0, 30) + "..." : question);
                    return new CachedAnswer(answer, source != null ? source : "SEMANTIC_CACHE", bestScore);
                }
            }
        } catch (Exception e) {
            log.warn("语义缓存查找失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 存入语义缓存
     */
    public void store(String question, String answer, String source) {
        try {
            if (redis.keys(KEY_PREFIX + "*").size() >= MAX_CACHED) {
                return; // 不无限增长
            }
            float[] vec = embeddingService.embed(question);
            String key = KEY_PREFIX + simpleHash(question);

            redis.opsForValue().set(key + ":vec", floatsToString(vec), TTL_HOURS, TimeUnit.HOURS);
            redis.opsForValue().set(key + ":ans", answer, TTL_HOURS, TimeUnit.HOURS);
            redis.opsForValue().set(key + ":src", source, TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("语义缓存存储失败: {}", e.getMessage());
        }
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB) + 1e-10);
    }

    private String floatsToString(float[] vec) {
        ByteBuffer buf = ByteBuffer.allocate(vec.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float v : vec) buf.putFloat(v);
        return Base64.getEncoder().encodeToString(buf.array());
    }

    private float[] bytesToFloats(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] vec = new float[bytes.length / 4];
        for (int i = 0; i < vec.length; i++) vec[i] = buf.getFloat();
        return vec;
    }

    private int simpleHash(String str) {
        int hash = 0;
        for (char c : str.toCharArray()) hash = 31 * hash + c;
        return Math.abs(hash);
    }

    /**
     * 缓存的回答
     */
    public record CachedAnswer(String answer, String source, double similarity) {}
}
