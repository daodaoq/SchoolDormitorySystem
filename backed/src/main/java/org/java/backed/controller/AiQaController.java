package org.java.backed.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.java.backed.common.PageResult;
import org.java.backed.common.Result;
import org.java.backed.entity.AiQaLog;
import org.java.backed.service.AiQaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiQaController {

    @Autowired
    private AiQaService aiQaService;

    /**
     * AI问答
     */
    @PostMapping("/ask")
    public Result<Map<String, Object>> ask(@RequestBody Map<String, String> params) {
        String userId = params.getOrDefault("userId", "anonymous");
        String question = params.get("question");
        if (question == null || question.trim().isEmpty()) {
            return Result.badRequest("问题不能为空");
        }
        Map<String, Object> result = aiQaService.ask(userId, question.trim());
        return Result.ok(result);
    }

    /**
     * 查询问答历史
     */
    @GetMapping("/history")
    public Result<PageResult<AiQaLog>> history(
            @RequestParam(defaultValue = "anonymous") String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<AiQaLog> result = aiQaService.queryHistory(page, pageSize, userId);
        return Result.ok(PageResult.from(result));
    }
}
