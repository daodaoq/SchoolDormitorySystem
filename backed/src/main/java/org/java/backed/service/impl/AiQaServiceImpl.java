package org.java.backed.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.entity.AiQaLog;
import org.java.backed.mapper.AiQaLogMapper;
import org.java.backed.service.AiQaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AiQaServiceImpl extends ServiceImpl<AiQaLogMapper, AiQaLog> implements AiQaService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired(required = false)
    private org.springframework.ai.openai.OpenAiChatClient aiChatClient;

    private static final int RATE_LIMIT_PER_MINUTE = 5;
    private static final String CACHE_KEY_PREFIX = "ai:qa:";
    private static final String RATE_LIMIT_KEY_PREFIX = "ai:rate:";

    @Override
    public Map<String, Object> ask(String userId, String question) {
        if (!checkRateLimit(userId)) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("question", question);
            result.put("answer", "您的问题过于频繁，请稍后再试。AI问答每分钟最多" + RATE_LIMIT_PER_MINUTE + "次。");
            result.put("source", "RATE_LIMIT");
            result.put("confidence", 0);
            return result;
        }

        String cacheKey = CACHE_KEY_PREFIX + simpleHash(question);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("question", question);
            result.put("answer", cached);
            result.put("source", "CACHE");
            result.put("confidence", 1.0);
            return result;
        }

        String localAnswer = matchLocalKB(question);
        if (localAnswer != null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("question", question);
            result.put("answer", localAnswer);
            result.put("source", "LOCAL_KB");
            result.put("confidence", 0.9);
            cacheAnswer(cacheKey, localAnswer);
            saveLog(userId, question, localAnswer, "LOCAL_KB", 0);
            return result;
        }

        String aiAnswer = null;
        long startTime = System.currentTimeMillis();
        try {
            if (aiChatClient != null) {
                String prompt = buildSystemPrompt() + "\n用户问题: " + question + "\n请用中文回答，简洁明了。";
                aiAnswer = aiChatClient.call(prompt);
            }
        } catch (Exception e) {
            log.error("AI调用异常", e);
        }

        int responseTime = (int) (System.currentTimeMillis() - startTime);

        if (aiAnswer == null || aiAnswer.isEmpty()) {
            aiAnswer = getFallbackAnswer(question);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("question", question);
            result.put("answer", aiAnswer);
            result.put("source", "FALLBACK");
            result.put("confidence", 0.5);
            saveLog(userId, question, aiAnswer, "FALLBACK", responseTime);
            return result;
        }

        cacheAnswer(cacheKey, aiAnswer);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("question", question);
        result.put("answer", aiAnswer);
        result.put("source", "AI");
        result.put("confidence", 0.85);
        saveLog(userId, question, aiAnswer, "AI", responseTime);
        return result;
    }

    @Override
    public Page<AiQaLog> queryHistory(int pageNum, int pageSize, String userId) {
        LambdaQueryWrapper<AiQaLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiQaLog::getUserId, userId);
        wrapper.orderByDesc(AiQaLog::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    private boolean checkRateLimit(String userId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) stringRedisTemplate.expire(key, 1, TimeUnit.MINUTES);
        return count == null || count <= RATE_LIMIT_PER_MINUTE;
    }

    private String matchLocalKB(String question) {
        String q = question.toLowerCase();
        if (containsAny(q, "入住", "怎么住", "住宿流程"))
            return "学生入住流程：1. 到宿舍管理处登记；2. 分配宿舍房间；3. 缴纳相关费用；4. 领取钥匙入住。如有疑问请咨询宿舍管理员。";
        if (containsAny(q, "收费", "费用", "多少钱", "价格"))
            return "宿舍收费项目包括：住宿费(1200元/学期)、水费(5元/吨)、电费(0.6元/度)、空调费(300元/学期)、网络费(50元/月)。具体费用以系统账单为准。";
        if (containsAny(q, "缴费", "支付", "怎么交", "付款"))
            return "缴费方式：登录系统后进入\"在线缴费\"页面，选择待缴账单，点击\"去支付\"跳转支付宝完成支付。支付成功后系统自动更新缴费状态。";
        if (containsAny(q, "逾期", "欠费", "没交", "罚款"))
            return "逾期未缴费会产生逾期记录，系统会通过短信/邮件发送逾期提醒。请尽快登录系统完成缴费，避免影响宿舍住宿资格。";
        if (containsAny(q, "退费", "退款", "多交"))
            return "退费申请流程：1. 联系宿舍管理员提交退费申请；2. 管理员核实缴费记录；3. 审核通过后原路退回支付账户。退费处理时间一般为3-5个工作日。";
        if (containsAny(q, "宿舍号", "换宿舍", "调换"))
            return "宿舍调换流程：1. 向宿舍管理员提交调换申请；2. 说明调换原因；3. 管理员审批；4. 审批通过后完成宿舍信息变更。";
        return null;
    }

    private String getFallbackAnswer(String question) {
        if (containsAny(question, "缴费", "支付"))
            return "如需缴费，请登录学生宿舍管理系统，进入\"在线缴费\"页面完成支付。如有疑问请联系宿舍管理员。";
        if (containsAny(question, "收费", "费用", "多少钱"))
            return "具体收费标准请查看系统中的\"收费项目\"页面，或联系宿舍管理员咨询。";
        return "感谢您的咨询。如需帮助，请联系宿舍管理员或查看系统帮助文档。";
    }

    private void cacheAnswer(String key, String answer) {
        stringRedisTemplate.opsForValue().set(key, answer, 1, TimeUnit.HOURS);
    }

    private void saveLog(String userId, String question, String answer, String source, int responseTime) {
        AiQaLog log = new AiQaLog();
        log.setUserId(userId);
        log.setQuestion(question);
        log.setAnswer(answer);
        log.setSource(source);
        log.setResponseTime(responseTime);
        save(log);
    }

    private String buildSystemPrompt() {
        return "你是学生宿舍收费管理系统的智能助手。你只能回答关于宿舍住宿、费用缴纳、收费项目、账单查询、缴费流程、逾期处理等系统业务相关问题。如果用户问题超出业务范围，请礼貌拒绝并引导用户咨询相关业务。";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) if (text.contains(keyword)) return true;
        return false;
    }

    private int simpleHash(String str) {
        int hash = 0;
        for (char c : str.toCharArray()) hash = 31 * hash + c;
        return Math.abs(hash);
    }
}
