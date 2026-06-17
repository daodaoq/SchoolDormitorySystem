package org.java.backed.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.java.backed.ai.dto.AiChatRequest;
import org.java.backed.common.PageResult;
import org.java.backed.common.Result;
import org.java.backed.entity.AiQaLog;
import org.java.backed.service.AiQaService;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * AI 智能问答控制器
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiQaController {

    private final AiQaService aiQaService;

    /**
     * 同步 AI 问答
     */
    @PostMapping("/ask")
    public Result<Map<String, Object>> ask(@Valid @RequestBody AiChatRequest request) {
        String userId = getUserId(request.getUserId());
        String question = request.getQuestion().trim();
        Map<String, Object> result = aiQaService.ask(userId, question);
        return Result.ok(result);
    }

    /**
     * 流式 AI 问答（SSE）
     */
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@Valid @RequestBody AiChatRequest request) {
        String userId = getUserId(request.getUserId());
        String question = request.getQuestion().trim();
        return aiQaService.askStream(userId, question);
    }

    /**
     * 查询问答历史
     */
    @GetMapping("/history")
    public Result<PageResult<AiQaLog>> history(
            @RequestParam(defaultValue = "anonymous") String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        // 尝试从认证上下文获取用户 ID
        String resolvedUserId = getUserId(userId);
        Page<AiQaLog> result = aiQaService.queryHistory(page, pageSize, resolvedUserId);
        return Result.ok(PageResult.from(result));
    }

    /**
     * AI 服务健康检查
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> status = aiQaService.getHealthStatus();
        return Result.ok(status);
    }

    /**
     * 从认证上下文获取当前用户 ID，降级使用请求参数
     */
    private String getUserId(String fallbackId) {
        try {
            Object principal = SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            if (principal != null && !"anonymousUser".equals(principal)) {
                // 如果 principal 是 SysUser 对象，取其 username；否则直接 toString
                return principal.toString();
            }
        } catch (Exception ignored) {
            // 认证不可用时降级
        }
        return fallbackId != null ? fallbackId : "anonymous";
    }
}
