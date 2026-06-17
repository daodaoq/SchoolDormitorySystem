package org.java.backed.ai.kb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.ai.config.AiProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话上下文管理器 — 滑动窗口 + AI 摘要
 * 参照 ragent 的 DefaultConversationMemoryService 设计
 *
 * 策略：
 * - 保留最近 4 轮对话原文
 * - 当超过 4 轮时，将最早的对话用 AI 浓缩为一句话摘要
 * - 摘要 + 最近 4 轮 = 完整上下文
 * - 上下文总长控制在 ~2000 tokens 以内
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationContextManager {

    private final ChatClient.Builder chatClientBuilder;
    private final AiProperties aiProperties;

    /** 滑动窗口大小 */
    private static final int WINDOW_SIZE = 4;

    /** 每个用户的对话缓存（userId → 对话列表） */
    private final Map<String, Deque<Turn>> conversations = new ConcurrentHashMap<>();

    /**
     * 添加一轮对话，返回压缩后的上下文字符串
     *
     * @param userId  用户 ID
     * @param question 用户问题
     * @param answer   AI 回答
     * @return 供下一轮 AI 调用使用的上下文字符串
     */
    public String addAndCompact(String userId, String question, String answer) {
        Deque<Turn> turns = conversations.computeIfAbsent(userId, k -> new ArrayDeque<>());
        turns.addLast(new Turn(question, answer));

        // 超过窗口大小，压缩最早的对话
        if (turns.size() > WINDOW_SIZE) {
            Turn oldest = turns.removeFirst();
            String summary = summarize(oldest);
            // 将摘要作为首条上下文
            turns.addFirst(new Turn("（历史摘要）", summary));
        }

        return buildContext(turns);
    }

    /**
     * 获取当前对话上下文
     */
    public String getContext(String userId) {
        Deque<Turn> turns = conversations.get(userId);
        if (turns == null || turns.isEmpty()) return "";
        return buildContext(turns);
    }

    /**
     * 清除用户对话历史
     */
    public void clear(String userId) {
        conversations.remove(userId);
    }

    /**
     * 用 AI 把一轮对话浓缩为一句话摘要
     */
    private String summarize(Turn turn) {
        try {
            String prompt = String.format(
                    "将以下问答浓缩为一句话摘要，只保留关键信息：\n问：%s\n答：%s\n\n一句话摘要：",
                    truncate(turn.question, 200),
                    truncate(turn.answer, 300)
            );
            String summary = chatClientBuilder.build()
                    .prompt().user(prompt).call().content();
            return summary != null ? summary.trim() : turn.question;
        } catch (Exception e) {
            log.warn("对话摘要失败，使用原文截断: {}", e.getMessage());
            return turn.question.substring(0, Math.min(100, turn.question.length()));
        }
    }

    private String buildContext(Deque<Turn> turns) {
        StringBuilder ctx = new StringBuilder();
        for (Turn t : turns) {
            ctx.append("Q: ").append(t.question).append("\n");
            ctx.append("A: ").append(t.answer).append("\n\n");
        }
        return ctx.toString().trim();
    }

    private String truncate(String text, int maxLen) {
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    /** 一轮对话 */
    private record Turn(String question, String answer) {}
}
