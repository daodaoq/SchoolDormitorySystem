package org.java.backed.ai.kb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 意图路由 — 根据用户问题识别意图，注入对应领域的增强 Prompt
 * 不用 AI 分类（省 token），纯规则匹配，快且免费
 */
@Slf4j
@Component
public class IntentRouter {

    /**
     * 识别用户意图并返回增强后的系统提示词
     *
     * @param question     用户问题
     * @param basePrompt   基础系统提示词
     * @return 增强后的提示词（基础 + 领域特化指令）
     */
    public String route(String question, String basePrompt) {
        Intent intent = classify(question);
        String domainPrompt = switch (intent) {
            case FEE_INQUIRY -> """

                    【收费咨询模式】
                    - 用户正在咨询收费标准，请优先从知识库中查找确切的收费金额
                    - 回答时列出具体收费项目和价格，使用表格格式更清晰
                    - 如知识库无确切金额，请明确告知"建议登录系统查看最新收费标准"
                    - 提醒用户不同宿舍类型收费标准可能不同""";

            case PAYMENT_HELP -> """

                    【缴费帮助模式】
                    - 用户需要缴费操作指导，请给出分步骤的操作说明
                    - 强调使用支付宝在线缴费，提及支付安全和凭证保存
                    - 如涉及具体账单，建议用户登录系统查看个人账单明细""";

            case DORMITORY_INFO -> """

                    【宿舍信息服务模式】
                    - 用户咨询宿舍相关信息，请给出准确的流程说明
                    - 涉及入住/调换等操作，列出完整步骤和所需材料
                    - 提醒用户具体宿舍分配以系统记录为准""";

            case OVERDUE -> """

                    【逾期处理模式】
                    - 用户可能涉及逾期未缴费，语气要关切但不制造焦虑
                    - 说明逾期影响和补救措施，强调尽快缴费的重要性
                    - 提供联系宿舍管理员的途径""";

            case REFUND -> """

                    【退费服务模式】
                    - 用户咨询退费事宜，详细说明退费条件和流程
                    - 说明退费处理时间（3-5个工作日）和退款方式
                    - 提醒用户准备相关证明材料""";

            case BILL_QUERY -> """

                    【账单查询模式】
                    - 用户需要查询账单信息，说明查看路径
                    - 解释账单状态含义（未缴/已缴/逾期）
                    - 建议用户登录系统获取实时账单数据""";

            case GENERAL -> "";
        };

        return basePrompt + domainPrompt;
    }

    /**
     * 规则分类（纯关键词匹配，不浪费 API）
     */
    private Intent classify(String question) {
        if (question == null) return Intent.GENERAL;
        String q = question.toLowerCase();

        // 优先级排序：具体匹配 → 模糊匹配
        if (containsAny(q, "退费", "退款", "退钱", "多交")) return Intent.REFUND;
        if (containsAny(q, "逾期", "欠费", "没交费", "罚款", "催缴", "超期")) return Intent.OVERDUE;
        if (containsAny(q, "多少钱", "价格", "收费", "费用", "收费标准", "计费")) return Intent.FEE_INQUIRY;
        if (containsAny(q, "怎么交", "如何交", "缴费", "支付", "付款", "支付宝", "在线缴费")) return Intent.PAYMENT_HELP;
        if (containsAny(q, "账单", "应缴", "未缴", "账单查询")) return Intent.BILL_QUERY;
        if (containsAny(q, "入住", "宿舍", "换宿舍", "调宿", "住宿", "宿舍信息")) return Intent.DORMITORY_INFO;

        return Intent.GENERAL;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) if (text.contains(kw)) return true;
        return false;
    }

    enum Intent { FEE_INQUIRY, PAYMENT_HELP, DORMITORY_INFO, OVERDUE, REFUND, BILL_QUERY, GENERAL }
}
