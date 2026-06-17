package org.java.backed.ai.kb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 查询重写服务
 * 将用户口语化问题改写为多个搜索优化的查询短语，提升向量检索召回率
 * 参照 ragent 的 MultiQuestionRewriteService 设计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryRewriteService {

    private final ChatClient.Builder chatClientBuilder;

    /** 纯中文短问题（<15字）用规则重写，不浪费 API */
    private static final Pattern SHORT_CN = Pattern.compile("^[\\u4e00-\\u9fa5\\d\\s？?！!，,。.、]{1,15}$");

    /**
     * 重写用户查询，返回原始查询 + 扩展查询
     *
     * @param query 原始用户问题
     * @return 查询列表（原始 + 扩展）
     */
    public List<String> rewrite(String query) {
        List<String> queries = new ArrayList<>();
        queries.add(query); // 原始查询始终保留

        if (query == null || query.trim().isEmpty()) return queries;

        // 短中文问题：用规则扩展关键词，快速且免费
        if (SHORT_CN.matcher(query.trim()).matches()) {
            List<String> expanded = ruleExpand(query.trim());
            if (!expanded.isEmpty()) {
                queries.addAll(expanded);
                log.debug("查询规则扩展: {} → {}", query, expanded);
                return queries;
            }
        }

        // 复杂问题：用 AI 生成搜索优化查询
        try {
            String aiRewrites = chatClientBuilder.build()
                    .prompt()
                    .user(String.format(
                            "将以下用户问题改写为2-3个搜索优化的查询短语，" +
                                    "每个短语应简洁、关键词密集、便于在知识库中检索到相关内容。" +
                                    "只输出查询短语，每行一个，不要编号，不要解释。\n用户问题：%s",
                            query))
                    .call()
                    .content();

            if (aiRewrites != null) {
                Arrays.stream(aiRewrites.split("\\n"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty() && !s.equals(query))
                        .forEach(queries::add);
                log.debug("查询 AI 扩展: {} → {} 个变体", query, queries.size() - 1);
            }
        } catch (Exception e) {
            log.warn("查询 AI 重写失败（使用原始查询）: {}", e.getMessage());
        }

        return queries;
    }

    /**
     * 规则扩展：基于关键词词典生成扩展查询
     */
    private List<String> ruleExpand(String query) {
        List<String> result = new ArrayList<>();

        if (query.contains("住宿") || query.contains("住")) {
            result.add("宿舍住宿费收费标准");
            result.add("住宿费用每学期多少钱");
        }
        if (query.contains("电费") || query.contains("电")) {
            result.add("电费收费标准每度多少钱");
        }
        if (query.contains("水费") || query.contains("水")) {
            result.add("水费收费标准每吨多少钱");
        }
        if (query.contains("缴费") || query.contains("支付") || query.contains("交")) {
            result.add("支付宝在线缴费流程");
            result.add("如何缴纳宿舍费用");
        }
        if (query.contains("退费") || query.contains("退款") || query.contains("退")) {
            result.add("宿舍费用退费申请流程");
            result.add("退费条件和退款到账时间");
        }
        if (query.contains("账单") || query.contains("查询")) {
            result.add("缴费账单查询方法");
            result.add("未缴已缴逾期账单状态");
        }
        if (query.contains("逾期") || query.contains("欠费") || query.contains("没交")) {
            result.add("逾期未缴费处理办法");
            result.add("逾期提醒通知机制");
        }
        if (query.contains("宿舍") && (query.contains("换") || query.contains("调") || query.contains("更改"))) {
            result.add("宿舍调换申请流程");
        }
        if (query.contains("网络") || query.contains("网费")) {
            result.add("校园网络费收费标准");
        }
        if (query.contains("空调")) {
            result.add("空调使用费收费标准");
        }

        return result;
    }
}
