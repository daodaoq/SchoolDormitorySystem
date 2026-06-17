package org.java.backed.ai.exception;

import lombok.Getter;

/**
 * AI 服务异常
 * 用于统一封装 AI 调用过程中的各类异常
 */
@Getter
public class AiServiceException extends RuntimeException {

    private final ErrorCode errorCode;

    public AiServiceException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AiServiceException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * AI 服务错误码枚举
     */
    @Getter
    public enum ErrorCode {
        /** 模型不可用（熔断打开） */
        CIRCUIT_OPEN(503, "AI 服务暂时不可用，请稍后再试"),
        /** 模型调用超时 */
        TIMEOUT(504, "AI 服务响应超时，请稍后再试"),
        /** 所有候选模型均调用失败 */
        ALL_MODELS_FAILED(502, "AI 服务异常，请稍后再试"),
        /** 请求被限流 */
        RATE_LIMITED(429, "请求过于频繁，请稍后再试"),
        /** 模型返回内容为空 */
        EMPTY_RESPONSE(502, "AI 服务返回内容为空");

        private final int httpStatus;
        private final String defaultMessage;

        ErrorCode(int httpStatus, String defaultMessage) {
            this.httpStatus = httpStatus;
            this.defaultMessage = defaultMessage;
        }
    }
}
