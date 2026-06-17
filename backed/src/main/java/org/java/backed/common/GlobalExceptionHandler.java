package org.java.backed.common;

import lombok.extern.slf4j.Slf4j;
import org.java.backed.ai.exception.AiServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return Result.badRequest(msg);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        return Result.badRequest(e.getMessage());
    }

    /**
     * AI 服务异常处理，根据错误码返回对应的 HTTP 状态码
     */
    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<Result<Void>> handleAiServiceException(AiServiceException e) {
        AiServiceException.ErrorCode code = e.getErrorCode();
        log.warn("AI 服务异常: code={}, message={}", code.name(), e.getMessage());
        HttpStatus status = HttpStatus.valueOf(code.getHttpStatus());
        return ResponseEntity.status(status)
                .body(Result.fail(code.getHttpStatus(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.fail("系统内部错误");
    }
}
