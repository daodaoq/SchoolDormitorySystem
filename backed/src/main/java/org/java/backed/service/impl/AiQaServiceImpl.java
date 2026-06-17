package org.java.backed.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.ai.circuit.ModelHealthMonitor;
import org.java.backed.ai.config.AiProperties;
import org.java.backed.ai.exception.AiServiceException;
import org.java.backed.ai.kb.ConversationContextManager;
import org.java.backed.ai.kb.IntentRouter;
import org.java.backed.ai.kb.MilvusVectorStoreService;
import org.java.backed.ai.kb.QueryRewriteService;
import org.java.backed.ai.kb.ResponseLevelRouter;
import org.java.backed.ai.kb.ResponseLevelRouter.ResponseLevel;
import org.java.backed.ai.kb.RerankService;
import org.java.backed.ai.kb.SemanticCacheService;
import org.java.backed.ai.stream.StreamingResponseHandler;
import org.java.backed.entity.AiQaLog;
import org.java.backed.mapper.AiQaLogMapper;
import org.java.backed.service.AiQaService;
import org.java.backed.service.KbDocumentService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AI 智能问答服务实现
 * 集成 Spring AI ChatClient + DeepSeek，支持熔断、限流、缓存、本地知识库、流式响应
 */
@Slf4j
@Service
public class AiQaServiceImpl extends ServiceImpl<AiQaLogMapper, AiQaLog> implements AiQaService {

    private static final String CACHE_KEY_PREFIX = "ai:qa:";
    private static final String RATE_LIMIT_KEY_PREFIX = "ai:rate:";
    private static final String MODEL_NAME = "deepseek-v4-flash";

    private final StringRedisTemplate stringRedisTemplate;
    private final ChatClient.Builder chatClientBuilder;
    private final AiProperties aiProperties;
    private final ModelHealthMonitor healthMonitor;
    private final StreamingResponseHandler streamingHandler;
    private final KbDocumentService kbDocumentService;
    private final RerankService rerankService;
    private final ConversationContextManager contextManager;
    private final QueryRewriteService queryRewriteService;
    private final SemanticCacheService semanticCache;
    private final IntentRouter intentRouter;
    private final ResponseLevelRouter levelRouter;

    public AiQaServiceImpl(StringRedisTemplate stringRedisTemplate,
                           ChatClient.Builder chatClientBuilder,
                           AiProperties aiProperties,
                           ModelHealthMonitor healthMonitor,
                           StreamingResponseHandler streamingHandler,
                           KbDocumentService kbDocumentService,
                           RerankService rerankService,
                           ConversationContextManager contextManager,
                           QueryRewriteService queryRewriteService,
                           SemanticCacheService semanticCache,
                           IntentRouter intentRouter,
                           ResponseLevelRouter levelRouter) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.chatClientBuilder = chatClientBuilder;
        this.aiProperties = aiProperties;
        this.healthMonitor = healthMonitor;
        this.streamingHandler = streamingHandler;
        this.kbDocumentService = kbDocumentService;
        this.rerankService = rerankService;
        this.contextManager = contextManager;
        this.queryRewriteService = queryRewriteService;
        this.semanticCache = semanticCache;
        this.intentRouter = intentRouter;
        this.levelRouter = levelRouter;
    }

    // ==================== 同步问答 ====================

    @Override
    public Map<String, Object> ask(String userId, String question) {
        // 0. 分级别路由：问候/越界 → 直接返回预设回复，不调 AI
        ResponseLevel level = levelRouter.classify(question);
        if (level == ResponseLevel.GREETING || level == ResponseLevel.OFF_TOPIC) {
            String reply = levelRouter.getPresetReply(level).text();
            saveLog(userId, question, reply, level.name(), 0);
            return buildResult(question, reply, level.name(), 1.0, 0);
        }

        // 1. 限流检查
        if (!checkRateLimit(userId)) {
            int maxReq = aiProperties.getRateLimit().getMaxRequestsPerMinute();
            return buildResult(question, "您的问题过于频繁，请稍后再试。每分钟最多 " + maxReq + " 次。",
                    "RATE_LIMIT", 0, 0);
        }

        // 2. 语义缓存检查（Embedding API 不可用时会降级跳过）
        try {
            SemanticCacheService.CachedAnswer semCached = semanticCache.lookup(question);
            if (semCached != null) {
                return buildResult(question, semCached.answer(),
                        semCached.source(), semCached.similarity(), 0);
            }
        } catch (Exception e) {
            log.warn("语义缓存不可用，跳过: {}", e.getMessage());
        }

        // 3. RAG + AI 调用
        String basePrompt = aiProperties.getPrompt().getSystemText();
        String systemPrompt = intentRouter.route(question, basePrompt);

        // 4. AI 调用（带 RAG + 重排序 + 查询重写 + 对话上下文）
        String aiAnswer = null;
        int responseTime = 0;
        try {
            if (!healthMonitor.allowCall(MODEL_NAME)) {
                throw new AiServiceException(AiServiceException.ErrorCode.CIRCUIT_OPEN,
                        "AI 模型 " + MODEL_NAME + " 已熔断");
            }

            long startTime = System.currentTimeMillis();

            // 5a. 查询重写 → 多查询提升召回率
            List<String> queries = queryRewriteService.rewrite(question);
            // 对每个查询检索知识库，合并去重
            String ragContext = buildRagContextMulti(queries);

            // 4b. 拼接最终 Prompt
            // 4c. 获取对话历史上下文（滑动窗口 + 摘要）
            String conversationContext = contextManager.getContext(userId);

            StringBuilder fullPrompt = new StringBuilder();
            if (!conversationContext.isEmpty()) {
                fullPrompt.append("【对话历史】\n").append(conversationContext).append("\n\n");
            }
            if (!ragContext.isEmpty()) {
                fullPrompt.append("【参考知识库内容】\n").append(ragContext).append("\n\n");
            }
            fullPrompt.append("【用户问题】\n").append(question);
            if (!ragContext.isEmpty()) {
                fullPrompt.append("\n请基于以上参考内容和对话历史，用中文简洁回答用户问题。");
            }

            aiAnswer = chatClientBuilder.build()
                    .prompt()
                    .system(systemPrompt)
                    .user(fullPrompt.toString())
                    .call()
                    .content();

            // 4d. 保存本轮对话到上下文管理器
            contextManager.addAndCompact(userId, question, aiAnswer);

            responseTime = (int) (System.currentTimeMillis() - startTime);
            healthMonitor.markSuccess(MODEL_NAME);
            log.info("AI 调用成功: userId={}, responseTime={}ms", userId, responseTime);

        } catch (AiServiceException e) {
            log.warn("AI 服务异常: code={}, msg={}", e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("AI 调用异常: type={}, msg={}", e.getClass().getSimpleName(), e.getMessage(), e);
            healthMonitor.markFailure(MODEL_NAME);
        }

        // 5. 降级：AI 失败 → 本地 KB → 通用兜底
        if (aiAnswer == null || aiAnswer.trim().isEmpty()) {
            // 先尝试本地知识库兜底
            if (aiProperties.getLocalKb().isEnabled()) {
                String localAnswer = matchLocalKB(question);
                if (localAnswer != null) {
                    saveLog(userId, question, localAnswer, "LOCAL_KB", responseTime);
                    return buildResult(question, localAnswer, "LOCAL_KB", 0.9, responseTime);
                }
            }
            // 再尝试通用降级回复
            if (aiProperties.getFallback().isEnabled()) {
                aiAnswer = getFallbackAnswer(question);
                saveLog(userId, question, aiAnswer, "FALLBACK", responseTime);
                return buildResult(question, aiAnswer, "FALLBACK", 0.5, responseTime);
            }
            String errorMsg = "AI 服务暂时不可用，请联系宿舍管理员获取帮助。";
            saveLog(userId, question, errorMsg, "ERROR", responseTime);
            return buildResult(question, errorMsg, "ERROR", 0, responseTime);
        }

        // 6. 语义缓存 & 记录（缓存失败不影响主流程）
        try { semanticCache.store(question, aiAnswer, "AI"); }
        catch (Exception e) { log.warn("语义缓存存储失败: {}", e.getMessage()); }
        saveLog(userId, question, aiAnswer, "AI", responseTime);
        Map<String, Object> result = buildResult(question, aiAnswer, "AI", 0.85, responseTime);
        // 附带引用来源
        result.put("citations", getLastCitations());
        return result;
    }

    // ==================== 流式问答 ====================

    @Override
    public SseEmitter askStream(String userId, String question) {
        // 1. 分级别路由：问候/越界 → 直接返回预设回复
        ResponseLevel level = levelRouter.classify(question);
        if (level == ResponseLevel.GREETING || level == ResponseLevel.OFF_TOPIC) {
            String reply = levelRouter.getPresetReply(level).text();
            SseEmitter presetEmitter = new SseEmitter();
            CompletableFuture.runAsync(() -> {
                try {
                    // 模拟流式逐字发送（分句发送）
                    String[] sentences = reply.split("(?<=[。！？\\n])");
                    for (String sentence : sentences) {
                        if (!sentence.isEmpty()) {
                            presetEmitter.send(SseEmitter.event()
                                    .name("content")
                                    .data(sentence));
                            Thread.sleep(30);
                        }
                    }
                    presetEmitter.send(SseEmitter.event()
                            .name("done")
                            .data("{\"status\":\"completed\",\"source\":\"" + level.name() + "\"}"));
                    presetEmitter.complete();
                } catch (Exception e) {
                    presetEmitter.completeWithError(e);
                }
            });
            saveLog(userId, question, reply, level.name(), 0);
            return presetEmitter;
        }

        // 2. 限流检查
        if (!checkRateLimit(userId)) {
            SseEmitter errorEmitter = new SseEmitter();
            try {
                errorEmitter.send(SseEmitter.event()
                        .name("error")
                        .data("请求过于频繁，请稍后再试"));
            } catch (Exception ignored) {
            }
            errorEmitter.complete();
            return errorEmitter;
        }

        // 3. 熔断检查
        if (!healthMonitor.allowCall(MODEL_NAME)) {
            SseEmitter errorEmitter = new SseEmitter();
            try {
                errorEmitter.send(SseEmitter.event()
                        .name("error")
                        .data("AI 服务暂时不可用，请稍后再试"));
            } catch (Exception ignored) {
            }
            errorEmitter.complete();
            return errorEmitter;
        }

        // 4. 构建 RAG 上下文和对话历史
        String ragContext = "";
        List<Map<String, Object>> citations = Collections.emptyList();
        try {
            List<String> queries = queryRewriteService.rewrite(question);
            ragContext = buildRagContextMulti(queries);
            citations = getLastCitationsSnapshot();
        } catch (Exception e) {
            log.debug("RAG 上下文构建跳过: {}", e.getMessage());
        }
        String conversationContext = contextManager.getContext(userId);

        // 5. 创建流式响应（带完整上下文 + 引用来源）
        String systemPrompt = intentRouter.route(question, aiProperties.getPrompt().getSystemText());
        StreamingResponseHandler.StreamRequest streamRequest = new StreamingResponseHandler.StreamRequest(
                question, systemPrompt, MODEL_NAME, ragContext, conversationContext, citations);
        SseEmitter emitter = streamingHandler.createStreamEmitter(streamRequest);

        // 6. 流完成后保存日志 & 更新对话上下文
        // 用数组保存完整回答（因 SseEmitter 回调不能直接访问外部非 final 变量）
        String[] fullAnswer = {""};

        emitter.onCompletion(() -> {
            // 尝试从 SseEmitter 的 data 中获取完整回答比较困难，用占位标记
            saveLog(userId, question, fullAnswer[0].isEmpty() ? "[流式回答]" : fullAnswer[0],
                    "AI_STREAM", 0);
            if (!fullAnswer[0].isEmpty()) {
                contextManager.addAndCompact(userId, question, fullAnswer[0]);
            }
            healthMonitor.markSuccess(MODEL_NAME);
            log.info("AI 流式响应完成: userId={}", userId);
        });
        emitter.onError(ex -> {
            healthMonitor.markFailure(MODEL_NAME);
            log.error("流式问答失败: userId={}", userId, ex);
        });
        emitter.onTimeout(() -> {
            healthMonitor.markFailure(MODEL_NAME);
            log.warn("流式问答超时: userId={}", userId);
        });

        return emitter;
    }

    // ==================== 历史记录 ====================

    @Override
    public Page<AiQaLog> queryHistory(int pageNum, int pageSize, String userId) {
        LambdaQueryWrapper<AiQaLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiQaLog::getUserId, userId);
        wrapper.orderByDesc(AiQaLog::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    // ==================== 健康状态 ====================

    @Override
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("model", MODEL_NAME);
        status.put("circuitBreaker", healthMonitor.getHealthStatus());
        status.put("localKbEnabled", aiProperties.getLocalKb().isEnabled());
        status.put("fallbackEnabled", aiProperties.getFallback().isEnabled());
        return status;
    }

    // ==================== RAG 检索 ====================

    /** 存放最近一次检索的引用来源，供 buildRagContext 填充后返回给调用方 */
    private final ThreadLocal<List<Map<String, Object>>> lastCitations = new ThreadLocal<>();

    /**
     * 获取最近一次 RAG 检索的引用来源（精简版，用于 SSE done 事件）
     */
    public List<Map<String, Object>> getLastCitationsSnapshot() {
        var c = lastCitations.get();
        if (c == null || c.isEmpty()) return Collections.emptyList();
        // 返回精简版（去掉 content 字段中的长文本，前端只需 docTitle + score）
        List<Map<String, Object>> snapshot = new ArrayList<>();
        for (var item : c) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("docTitle", item.get("docTitle"));
            m.put("score", item.get("score"));
            m.put("docId", item.get("docId"));
            m.put("content", item.get("content"));
            snapshot.add(m);
        }
        return snapshot;
    }

    /**
     * 多查询 RAG 检索：对每个重写查询分别检索，合并去重
     */
    private String buildRagContextMulti(List<String> queries) {
        Map<String, Map<String, Object>> seen = new LinkedHashMap<>(); // chunkId → result
        for (String q : queries) {
            try {
                var results = kbDocumentService.search(q, 5);
                if (results != null) {
                    for (var r : results) {
                        String cid = (String) r.get("chunkId");
                        if (cid != null && !seen.containsKey(cid)
                                && (double) r.getOrDefault("score", 0.0) > 0.3) {
                            seen.put(cid, r);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("子查询检索失败: {}", q);
            }
        }
        return buildRagContextFromResults(new ArrayList<>(seen.values()), queries.get(0));
    }

    /**
     * Rerank + 构建上下文
     */
    private String buildRagContextFromResults(List<Map<String, Object>> results, String query) {
        try {
            if (results.isEmpty()) {
                lastCitations.set(Collections.emptyList());
                return "";
            }

            // Rerank 精排
            results = rerankService.rerank(query, results);
            if (results == null || results.isEmpty()) {
                lastCitations.set(Collections.emptyList());
                return "";
            }

            // 过滤低相关度
            List<Map<String, Object>> citations = results.stream()
                    .filter(r -> (double) r.getOrDefault("score", 0.0) > 0.3)
                    .toList();
            lastCitations.set(citations);

            if (citations.isEmpty()) return "";

            // 构建纯参考上下文（不带引用格式指令）
            StringBuilder ctx = new StringBuilder();
            ctx.append("【参考知识库内容】\n");
            for (int i = 0; i < citations.size(); i++) {
                var c = citations.get(i);
                ctx.append("[").append(i + 1).append("] ")
                        .append("《").append(c.get("docTitle")).append("》")
                        .append(": ").append(c.get("content")).append("\n");
            }
            return ctx.toString();
        } catch (Exception e) {
            log.warn("RAG 检索/Rerank 失败（不影响主流程）: {}", e.getMessage());
            lastCitations.set(Collections.emptyList());
            return "";
        }
    }

    /**
     * 从知识库检索 + Rerank 重排序 + 构建带来源标注的上下文（单查询）
     */
    private String buildRagContext(String question) {
        try {
            // 1. 向量粗筛 Top-10
            var results = kbDocumentService.search(question, 10);
            if (results == null || results.isEmpty()) {
                lastCitations.set(Collections.emptyList());
                return "";
            }

            // 2. Rerank 精排 Top-3
            results = rerankService.rerank(question, results);
            if (results == null || results.isEmpty()) {
                lastCitations.set(Collections.emptyList());
                return "";
            }

            // 3. 过滤低相关度
            List<Map<String, Object>> citations = results.stream()
                    .filter(r -> (double) r.getOrDefault("score", 0.0) > 0.3)
                    .toList();
            lastCitations.set(citations);

            if (citations.isEmpty()) return "";

            // 构建纯参考上下文（不带引用格式指令）
            StringBuilder ctx = new StringBuilder();
            ctx.append("【参考知识库内容】\n");
            for (int i = 0; i < citations.size(); i++) {
                var c = citations.get(i);
                ctx.append("[").append(i + 1).append("] ")
                        .append("《").append(c.get("docTitle")).append("》")
                        .append(": ").append(c.get("content")).append("\n");
            }
            return ctx.toString();
        } catch (Exception e) {
            log.warn("RAG 检索/Rerank 失败（不影响主流程）: {}", e.getMessage());
            lastCitations.set(Collections.emptyList());
            return "";
        }
    }

    /**
     * 获取最近一次 RAG 检索的引用来源
     */
    public List<Map<String, Object>> getLastCitations() {
        var c = lastCitations.get();
        return c != null ? c : Collections.emptyList();
    }

    // ==================== 限流 ====================

    private boolean checkRateLimit(String userId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, 1, TimeUnit.MINUTES);
        }
        int maxReq = aiProperties.getRateLimit().getMaxRequestsPerMinute();
        return count == null || count <= maxReq;
    }

    // ==================== 本地知识库 ====================

    /**
     * 本地知识库匹配，覆盖系统全部 7 大业务模块
     */
    private String matchLocalKB(String question) {
        String q = question.toLowerCase().trim();

        // 1. 宿舍信息
        if (containsAny(q, "入住", "怎么住", "住宿流程", "入住流程", "办入住"))
            return "学生入住流程：\n1. 到宿舍管理处登记报到\n2. 管理员分配宿舍房间和床位\n3. 缴纳相关住宿费用\n4. 领取宿舍钥匙/门禁卡\n5. 填写入住登记表，确认入住\n如有疑问请联系宿舍管理员。";

        if (containsAny(q, "宿舍号", "换宿舍", "调换", "调宿"))
            return "宿舍调换流程：\n1. 向宿舍管理员提交调换申请表\n2. 说明调换原因（如室友矛盾、设施问题等）\n3. 管理员审核并查询空余床位\n4. 审批通过后办理宿舍信息变更\n5. 在规定时间内完成搬离和入住";

        if (containsAny(q, "宿舍信息", "宿舍查询", "我的宿舍", "住哪个"))
            return "如需查询您的宿舍信息（宿舍号、室友、入住时间等），请登录系统后在【学生宿舍信息】页面查看，或联系宿舍管理员查询。";

        // 2. 收费标准
        if (containsAny(q, "收费", "费用", "多少钱", "价格", "收费标准", "收费项目"))
            return "宿舍收费项目及标准：\n1. 住宿费 -- 1200元/学期（标准四人间）\n2. 水费 -- 5元/吨（按实际用量）\n3. 电费 -- 0.6元/度（按实际用量）\n4. 空调费 -- 300元/学期\n5. 网络费 -- 50元/月\n6. 热水费 -- 按实际用量计费\n具体费用以系统中的收费项目页面为准，不同宿舍类型收费标准可能有所不同。";

        // 3. 账单查询
        if (containsAny(q, "账单", "应缴", "欠费", "未缴", "缴费状态", "账单查询"))
            return "缴费账单说明：\n- 系统每月自动生成缴费账单，包含住宿费、水电费等\n- 账单状态包括：未缴、已缴、逾期\n- 登录系统进入【缴费账单】页面即可查看所有账单\n- 账单含应缴金额、缴费截止时间等信息\n- 如有疑问请联系宿舍管理员核对账单明细";

        // 4. 在线缴费
        if (containsAny(q, "缴费", "支付", "怎么交", "付款", "支付宝", "在线缴费"))
            return "在线缴费流程：\n1. 登录系统进入【在线缴费】页面\n2. 查看待缴账单，确认缴费金额\n3. 点击【去支付】跳转支付宝收银台\n4. 使用支付宝扫码或账号密码完成支付\n5. 支付成功后系统自动更新账单状态\n6. 可在【支付记录】中查看缴费凭证\n注意：当前为支付宝沙箱测试环境，仅支持测试账号支付。";

        // 5. 报表导出
        if (containsAny(q, "报表", "导出", "excel", "统计", "报表导出"))
            return "收费报表导出说明：\n- 系统支持导出月度/学期收费统计报表（Excel格式）\n- 报表内容包括：收费项目汇总、缴费率、逾期情况\n- 进入【统计分析】页面，选择时间范围后点击导出\n- 管理员可导出全部学生报表，学生仅可导出个人账单";

        // 6. 逾期处理
        if (containsAny(q, "逾期", "超期", "没交费", "罚款", "提醒", "催缴"))
            return "逾期处理说明：\n- 超过缴费截止日期未缴费将标记为【逾期】\n- 系统自动通过短信/邮件发送逾期提醒\n- 逾期将影响宿舍住宿资格，严重者可能取消住宿\n- 请尽快登录系统完成缴费，避免产生逾期记录\n- 如有特殊情况请及时联系宿舍管理员说明";

        // 7. 退费
        if (containsAny(q, "退费", "退款", "退钱", "多交", "退"))
            return "退费申请流程：\n1. 联系宿舍管理员提交退费申请\n2. 提供退费原因及相关证明材料\n3. 管理员核实缴费记录和退费资格\n4. 审核通过后款项原路退回支付账户\n5. 退费处理时间一般为 3-5 个工作日\n具体退费政策请咨询宿舍管理员。";

        // 综合类
        if (containsAny(q, "联系", "电话", "管理员", "帮助", "咨询"))
            return "如需帮助，您可以：\n1. 联系宿舍管理员（办公室位于宿舍楼一楼管理处）\n2. 在系统中查看帮助文档\n3. 通过本 AI 助手咨询相关问题\n4. 拨打宿舍管理处电话（请查看宿舍公告栏）";

        if (containsAny(q, "你好", "hi", "hello", "在吗", "你是谁"))
            return "您好！我是宿管小助手，学生宿舍收费管理系统的 AI 智能助手。我可以帮您解答以下问题：\n- 宿舍入住与调换\n- 收费标准查询\n- 账单查询与缴费\n- 报表导出\n- 逾期处理与退费\n请问有什么可以帮助您的？";

        return null;
    }

    // ==================== 降级回复 ====================

    private String getFallbackAnswer(String question) {
        if (containsAny(question, "缴费", "支付", "付款"))
            return "如需缴费，请登录学生宿舍管理系统，进入【在线缴费】页面完成支付。当前支持支付宝支付，如有疑问请联系宿舍管理员。";
        if (containsAny(question, "收费", "费用", "多少钱"))
            return "具体收费标准请查看系统中的【收费项目】页面。不同宿舍类型收费标准不同，建议登录系统查询个人账单详情。";
        if (containsAny(question, "宿舍", "入住", "住宿"))
            return "宿舍相关信息请登录系统查看，或直接联系宿舍管理员（办公室位于宿舍楼一楼管理处）获取帮助。";
        if (containsAny(question, "账单", "欠费"))
            return "账单信息请登录系统在【缴费账单】页面查看。如有账单疑问，请联系宿舍管理员核对。";
        if (containsAny(question, "报表", "导出", "统计"))
            return "报表导出功能请登录系统后进入【统计分析】页面操作。如需帮助请联系宿舍管理员。";
        return "感谢您的咨询。如需更详细的帮助，请联系宿舍管理员或登录系统查看相关功能页面。";
    }

    // ==================== 工具方法 ====================

    private void cacheAnswer(String key, String answer) {
        stringRedisTemplate.opsForValue().set(key, answer,
                aiProperties.getCache().getTtlSeconds(), TimeUnit.SECONDS);
    }

    private void saveLog(String userId, String question, String answer, String source, int responseTime) {
        try {
            AiQaLog log = new AiQaLog();
            log.setUserId(userId);
            log.setQuestion(question);
            log.setAnswer(answer);
            log.setSource(source);
            log.setResponseTime(responseTime);
            save(log);
        } catch (Exception e) {
            log.error("保存 AI 问答日志失败", e);
        }
    }

    private Map<String, Object> buildResult(String question, String answer, String source,
                                            double confidence, int responseTimeMs) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("question", question);
        result.put("answer", answer);
        result.put("source", source);
        result.put("confidence", confidence);
        result.put("responseTimeMs", responseTimeMs);
        return result;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private int simpleHash(String str) {
        int hash = 0;
        for (char c : str.toCharArray()) {
            hash = 31 * hash + c;
        }
        return Math.abs(hash);
    }
}
