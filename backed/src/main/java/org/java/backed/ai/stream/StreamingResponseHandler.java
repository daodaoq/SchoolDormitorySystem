package org.java.backed.ai.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.ai.config.AiProperties;
import org.java.backed.ai.dto.CitationItem;
import org.java.backed.ai.kb.CitationParser;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 流式响应处理器
 * 将 Spring AI 的 Flux<String> 流式输出桥接到 Servlet SSE (SseEmitter)
 * 支持 RAG 上下文、对话历史、Markdown 格式回答
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreamingResponseHandler {

    private final ChatClient.Builder chatClientBuilder;
    private final AiProperties aiProperties;
    private final CitationParser citationParser;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 流式请求参数
     */
    public record StreamRequest(
            String question,
            String systemPrompt,
            String modelName,
            String ragContext,
            String conversationContext,
            List<Map<String, Object>> citations,
            List<CitationItem> rawCitationItems
    ) {
        public StreamRequest(String question, String systemPrompt, String modelName,
                             String ragContext, String conversationContext) {
            this(question, systemPrompt, modelName, ragContext, conversationContext,
                 Collections.emptyList(), Collections.emptyList());
        }
    }

    /**
     * 创建流式 SSE 响应（完整参数）
     */
    public SseEmitter createStreamEmitter(StreamRequest request) {
        AiProperties.Stream streamConfig = aiProperties.getStream();
        long timeoutMs = streamConfig.getTimeoutSeconds() * 1000L;
        long heartbeatMs = streamConfig.getHeartbeatIntervalMs();

        SseEmitter emitter = new SseEmitter(timeoutMs);

        String streamId = UUID.randomUUID().toString().substring(0, 8);
        String shortQuestion = request.question().length() > 50
                ? request.question().substring(0, 50) + "..."
                : request.question();
        log.info("创建 AI 流式响应: streamId={}, question={}, hasRag={}, hasCtx={}, citations={}",
                streamId, shortQuestion,
                !request.ragContext().isEmpty(),
                !request.conversationContext().isEmpty(),
                request.citations().size());

        // 异步执行流式调用，避免阻塞 Servlet 线程
        CompletableFuture.runAsync(() -> {
            try {
                String fullUserPrompt = buildFullUserPrompt(request);
                String enhancedSystemPrompt = enhanceSystemPrompt(request.systemPrompt());

                Flux<String> contentFlux = chatClientBuilder.build()
                        .prompt()
                        .system(enhancedSystemPrompt)
                        .user(fullUserPrompt)
                        .stream()
                        .content();

                StringBuilder fullContent = new StringBuilder();
                Thread heartbeatThread = startHeartbeat(emitter, heartbeatMs, streamId);

                try {
                    contentFlux
                            .doOnNext(chunk -> {
                                fullContent.append(chunk);
                                sendEvent(emitter, "content", chunk);
                            })
                            .doOnComplete(() -> {
                                log.info("AI 流式响应完成: streamId={}, 总长度={}", streamId, fullContent.length());
                                // 用 CitationParser 解析行内引用标记，生成增强引用
                                List<CitationItem> enrichedCitations = citationParser.parse(
                                        fullContent.toString(), request.rawCitationItems());
                                sendDoneEventWithEnrichedCitations(emitter, enrichedCitations, request.citations());
                                emitter.complete();
                            })
                            .doOnError(error -> {
                                log.error("AI 流式响应异常: streamId={}", streamId, error);
                                sendEvent(emitter, "error", "AI 服务异常: " + error.getMessage());
                                emitter.completeWithError(error);
                            })
                            .blockLast();
                } finally {
                    heartbeatThread.interrupt();
                }

            } catch (Exception e) {
                log.error("AI 流式调用失败: streamId={}", streamId, e);
                sendEvent(emitter, "error", "AI 服务调用失败: " + e.getMessage());
                emitter.completeWithError(e);
            }
        });

        emitter.onCompletion(() -> log.debug("SSE 连接完成: streamId={}", streamId));
        emitter.onTimeout(() -> log.warn("SSE 连接超时: streamId={}", streamId));
        emitter.onError(ex -> log.error("SSE 连接异常: streamId={}", streamId, ex));

        return emitter;
    }

    /**
     * 创建流式 SSE 响应（简化版，仅问题 + 系统提示词）
     */
    public SseEmitter createStreamEmitter(String question, String systemPrompt, String modelName) {
        return createStreamEmitter(new StreamRequest(question, systemPrompt, modelName, "", ""));
    }

    /**
     * 发送 done 事件，包含增强的引用来源（行内引用解析后）
     */
    private void sendDoneEventWithEnrichedCitations(SseEmitter emitter, List<CitationItem> enrichedCitations,
                                                     List<Map<String, Object>> fallbackCitations) {
        Map<String, Object> doneData = new LinkedHashMap<>();
        doneData.put("status", "completed");
        List<Map<String, Object>> citationMaps = CitationParser.toMapList(enrichedCitations);
        if (!citationMaps.isEmpty()) {
            doneData.put("citations", citationMaps);
        } else if (fallbackCitations != null && !fallbackCitations.isEmpty()) {
            // 如果解析后为空但 RAG 有结果，使用原始引用（标记为未引用）
            doneData.put("citations", fallbackCitations);
        }
        try {
            sendEvent(emitter, "done", objectMapper.writeValueAsString(doneData));
        } catch (JsonProcessingException e) {
            sendEvent(emitter, "done", "{\"status\":\"completed\"}");
        }
    }

    /**
     * 发送 done 事件，包含引用来源
     */
    private void sendDoneEvent(SseEmitter emitter, List<Map<String, Object>> citations) {
        Map<String, Object> doneData = new LinkedHashMap<>();
        doneData.put("status", "completed");
        if (citations != null && !citations.isEmpty()) {
            doneData.put("citations", citations);
        }
        try {
            sendEvent(emitter, "done", objectMapper.writeValueAsString(doneData));
        } catch (JsonProcessingException e) {
            sendEvent(emitter, "done", "{\"status\":\"completed\"}");
        }
    }

    /**
     * 拼接完整用户 Prompt：对话历史 + RAG 参考内容 + 用户问题
     */
    private String buildFullUserPrompt(StreamRequest request) {
        StringBuilder prompt = new StringBuilder();

        if (!request.conversationContext().isEmpty()) {
            prompt.append("【对话历史】\n").append(request.conversationContext()).append("\n\n");
        }
        if (!request.ragContext().isEmpty()) {
            prompt.append("【参考知识库内容】\n").append(request.ragContext()).append("\n\n");
        }

        prompt.append("【用户问题】\n").append(request.question());

        if (!request.ragContext().isEmpty()) {
            prompt.append("\n请基于以上参考内容和对话历史，用中文简洁回答用户问题。回答中务必使用[N]标记引用参考内容。");
        }

        return prompt.toString();
    }

    /**
     * 增强系统提示词，追加 Markdown 格式要求 + 引用格式要求
     */
    private String enhanceSystemPrompt(String basePrompt) {
        String instruction = """

                【回答格式要求 — 必须严格遵守】
                使用 Markdown 组织回答，注意：所有标记符后面必须有一个空格！
                - 标题：「## 标题文字」（## 后必须有空格）
                - 有序列表：「1. 项目」（数字+点号后必须有空格）
                - 无序列表：「- 项目」（减号后必须有空格）
                - 粗体：「**关键词**」（双星号包裹，前后不留空格）
                - 引用块：「> 引用文字」（> 后必须有空格）
                - 保持简洁清晰，不要使用表格

                【引用格式要求 — 必须严格遵守】
                - 当你的回答参考了知识库内容时，必须在引用处标注来源编号
                - 格式：直接在被引用的句子后面紧接[N]标记
                - 例如："根据规定，住宿费为1200元/学期[1]。缴费可通过支付宝完成[2]。"
                - 不要在[N]和前面的文字之间加空格""";
        return basePrompt + instruction;
    }

    /**
     * 启动心跳线程，定期发送注释事件防止代理超时
     */
    private Thread startHeartbeat(SseEmitter emitter, long intervalMs, String streamId) {
        Thread thread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(intervalMs);
                    try {
                        emitter.send(SseEmitter.event().comment("heartbeat"));
                    } catch (IOException e) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                // 正常退出
            }
        }, "sse-heartbeat-" + streamId);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private void sendEvent(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            log.debug("SSE 发送失败（连接可能已关闭）: event={}", eventName);
        }
    }
}
