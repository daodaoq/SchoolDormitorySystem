package org.java.backed.common;

import lombok.Data;

/**
 * 统一响应结果封装
 */
@Data
public class Result<T> {

    private int code;
    private String message;
    private T data;

    private Result() {}

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ========== 成功响应 ==========

    public static <T> Result<T> ok() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(200, message, data);
    }

    // ========== 失败响应 ==========

    public static <T> Result<T> fail() {
        return new Result<>(500, "fail", null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    // ========== 常用错误 ==========

    public static <T> Result<T> badRequest(String message) {
        return new Result<>(400, message, null);
    }

    public static <T> Result<T> notFound(String message) {
        return new Result<>(404, message, null);
    }

    public static <T> Result<T> conflict(String message) {
        return new Result<>(409, message, null);
    }

    public static <T> Result<T> tooManyRequests(String message) {
        return new Result<>(429, message, null);
    }
}
