package org.java.backed.ai.kb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 分级别响应路由器
 * 根据问题类型决定响应策略，节省 AI 调用成本
 *
 * Level 1 — 问候语：直接返回固定话术，不调 AI
 * Level 2 — 越界问题：礼貌拒绝，不调知识库
 * Level 3 — 业务问题：全流程 RAG + AI
 */
@Slf4j
@Component
public class ResponseLevelRouter {

    /** 问候语关键词 */
    private static final Set<String> GREETINGS = Set.of(
            "你好", "hi", "hello", "嗨", "在吗", "在不在", "早上好", "下午好", "晚上好",
            "你是谁", "你能做什么", "你有什么功能", "介绍一下自己"
    );

    /** 越界关键词（和宿舍收费完全无关的话题） */
    private static final Pattern OFF_TOPIC = Pattern.compile(
            "天气|股票|基金|比特币|游戏|王者|吃鸡|电影|明星|综艺|足球|篮球|世界杯|" +
                    "编程|代码|java|python|前端|后端|面试|简历|相亲|恋爱|减肥|健身|" +
                    "旅游|景点|美食|餐厅|外卖|快递|网购|淘宝|京东|拼多多|" +
                    "政治|政府|选举|宗教|色情|暴力|违法|犯罪"
    );

    /** 纯标点/空格 */
    private static final Pattern BLANK = Pattern.compile("^[\\s\\p{Punct}]*$");

    /** 业务相关关键词（命中任意一个即视为正常问题） */
    private static final Pattern BUSINESS = Pattern.compile(
            "宿舍|住宿|入住|退宿|收费|费用|缴费|支付|账单|欠费|逾期|退费|退款|" +
                    "水电|电费|水费|空调|网络|网费|热水|暖气|洗澡|" +
                    "学生|学号|房间|床位|调换|申请|流程|怎么|如何|" +
                    "标准|价格|多少钱|什么|查询|查看|帮助|联系|管理员|" +
                    "寝室|舍友|卫生|安全|门禁|钥匙|报修|维修"
    );

    /**
     * 判断响应级别
     */
    public ResponseLevel classify(String question) {
        if (question == null || question.trim().isEmpty() || BLANK.matcher(question).matches()) {
            return ResponseLevel.GREETING;
        }

        String q = question.trim().toLowerCase();

        // Level 1: 问候
        for (String g : GREETINGS) {
            if (q.contains(g)) {
                return ResponseLevel.GREETING;
            }
        }

        // Level 2: 越界
        if (OFF_TOPIC.matcher(q).find() && !BUSINESS.matcher(q).find()) {
            return ResponseLevel.OFF_TOPIC;
        }

        // Level 3: 业务问题
        return ResponseLevel.BUSINESS;
    }

    /**
     * 获取对应级别的预设回复
     */
    public PresetReply getPresetReply(ResponseLevel level) {
        return switch (level) {
            case GREETING -> PresetReply.GREETING;
            case OFF_TOPIC -> PresetReply.OFF_TOPIC;
            case BUSINESS -> null; // 走正常流程
        };
    }

    public enum ResponseLevel { GREETING, OFF_TOPIC, BUSINESS }

    public enum PresetReply {
        GREETING("""
                您好！我是宿管小助手 👋
                我可以帮您解答以下问题：
                - 📋 宿舍入住与调换流程
                - 💰 收费项目及标准查询
                - 🧾 缴费账单查询
                - 💳 在线缴费操作指导
                - 📊 报表导出说明
                - ⚠️ 逾期处理与退费申请

                请问有什么可以帮助您的？"""),

        OFF_TOPIC("""
                抱歉，我是学生宿舍收费管理系统的专属助手，只能回答与宿舍住宿、费用缴纳、账单查询等相关的问题。
                如果您有这些方面的疑问，请随时告诉我！""");

        private final String text;

        PresetReply(String text) { this.text = text; }
        public String text() { return text; }
    }
}
