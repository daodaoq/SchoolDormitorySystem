package org.java.backed.ai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 阿里云百炼 Embedding 独立配置
 * Chat  → DeepSeek（spring.ai.openai 自动配置）
 * Embed → 阿里云百炼（本配置类）
 */
@Configuration
public class BailianEmbeddingConfig {

    @Value("${bailian.api-key}")
    private String apiKey;

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        OpenAiApi bailianApi = OpenAiApi.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKey(apiKey)
                .build();
        return new OpenAiEmbeddingModel(bailianApi);
    }
}
