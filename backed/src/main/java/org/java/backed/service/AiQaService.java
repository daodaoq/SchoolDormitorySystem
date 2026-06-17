package org.java.backed.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.java.backed.entity.AiQaLog;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * AI 智能问答服务接口
 * 支持同步问答、流式问答、历史记录查询
 */
public interface AiQaService extends IService<AiQaLog> {

    /**
     * 同步问答（非流式）
     *
     * @param userId   用户 ID
     * @param question 用户问题
     * @return 问答结果，包含 answer、source、confidence、responseTimeMs 等字段
     */
    Map<String, Object> ask(String userId, String question);

    /**
     * 流式问答（SSE）
     *
     * @param userId   用户 ID
     * @param question 用户问题
     * @return SseEmitter SSE 发射器，前端通过 EventSource 接收流式内容
     */
    SseEmitter askStream(String userId, String question);

    /**
     * 查询问答历史记录
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param userId   用户 ID
     * @return 分页结果
     */
    Page<AiQaLog> queryHistory(int pageNum, int pageSize, String userId);

    /**
     * 获取 AI 模型健康状态
     *
     * @return 健康状态信息
     */
    Map<String, Object> getHealthStatus();
}
