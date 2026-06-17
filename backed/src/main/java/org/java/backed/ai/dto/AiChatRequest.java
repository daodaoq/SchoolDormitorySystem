package org.java.backed.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 问答请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {

    /** 用户问题 */
    @NotBlank(message = "问题不能为空")
    private String question;

    /** 用户 ID（可选，优先从认证上下文获取） */
    private String userId;
}
