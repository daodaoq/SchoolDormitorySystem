package org.java.backed.ai.stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.ai.config.AiProperties;
import org.java.backed.ai.exception.AiServiceException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 流式响应处理器
 * 将 Spring AI 的 Flux<String> 流式输出桥接到 Servlet SSE (SseEmitter)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreamingResponseHandler {

    private final ChatClient.Builder chatClientBuilder;
    private final AiProperties aiProperties;

    /**
     * 创建流式 SSE 响应
     *
     * @param question     用户问题
     * @param systemPrompt 系统提示词
     * @param modelName    模型名称
     * @return SseEmitter SSE 发射器
     */
    public SseEmitter createStreamEmitter(String question, String systemPrompt, String modelName) {
        AiProperties.Stream streamConfig = aiProperties.getStream();
        long timeoutMs = streamConfig.getTimeoutSeconds() * 1000L;
        long heartbeatMs = streamConfig.getHeartbeatIntervalMs();

        SseEmitter emitter = new SseEmitter(timeoutMs);

        String streamId = UUID.randomUUID().toString().substring(0, 8);
        log.info("创建 AI 流式响应: streamId={}, question={}", streamId,
                question.length() > 50 ? question.substring(0, 50) + "..." : question);

        // 异步执行流式调用，避免阻塞 Servlet 线程
        CompletableFuture.runAsync(() -> {
            try {
                Flux<String> contentFlux = chatClientBuilder.build()
                        .prompt()
                        .system(systemPrompt)
                        .user(question)
                        .stream()
                        .content();

                // 使用数组包装 StringBuilder，因为 lambda 内需要可变引用
                StringBuilder fullContent = new StringBuilder();
                boolean[] contentReceived = {false};

                // 启动心跳线程
                Thread heartbeatThread = startHeartbeat(emitter, heartbeatMs, streamId);

                try {
                    // 阻塞订阅 Flux（在异步线程中执行）
                    contentFlux
                            .doOnNext(chunk -> {
                                contentReceived[0] = true;
                                fullContent.append(chunk);
                                sendEvent(emitter, "content", chunk);
                            })
                            .doOnComplete(() -> {
                                log.info("AI 流式响应完成: streamId={}, 总长度={}", streamId, fullContent.length());
                                sendEvent(emitter, "done", "completed");
                                emitter.complete();
                            })
                            .doOnError(error -> {
                                log.error("AI 流式响应异常: streamId={}", streamId, error);
                                sendEvent(emitter, "error", "AI 服务异常: " + error.getMessage());
                                emitter.completeWithError(error);
                            })
                            .blockLast(); // 阻塞当前异步线程直到流结束
                } finally {
                    heartbeatThread.interrupt();
                }

            } catch (Exception e) {
                log.error("AI 流式调用失败: streamId={}", streamId, e);
                sendEvent(emitter, "error", "AI 服务调用失败: " + e.getMessage());
                emitter.completeWithError(e);
            }
        });

        // 注册完成/超时/错误回调
        emitter.onCompletion(() -> log.debug("SSE 连接完成: streamId={}", streamId));
        emitter.onTimeout(() -> log.warn("SSE 连接超时: streamId={}", streamId));
        emitter.onError(ex -> log.error("SSE 连接异常: streamId={}", streamId, ex));

        return emitter;
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
                        // 连接已关闭，退出心跳
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
