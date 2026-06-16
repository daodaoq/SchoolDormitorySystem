package org.java.backed.util;

import lombok.extern.slf4j.Slf4j;
import org.java.backed.config.AlipayConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 支付宝支付工具类 (Stub实现 - 需要集成真实Alipay SDK后替换)
 *
 * 集成步骤:
 * 1. 下载 alipay-sdk-java 并安装到本地Maven仓库
 * 2. 取消pom.xml中Alipay SDK依赖的 provided/optional 标记
 * 3. 替换此文件中的Stub实现为真实SDK调用
 */
@Slf4j
@Component
public class AlipayUtil {

    @Autowired
    private AlipayConfig alipayConfig;

    /**
     * 创建支付页面
     */
    public String createPayPage(String orderNo, String subject, String body, String amount) {
        if (!alipayConfig.isConfigured()) {
            log.warn("支付宝沙箱未配置，返回模拟支付页面。orderNo={}", orderNo);
            return generateMockPayForm(orderNo, subject, amount);
        }
        // TODO: 集成真实Alipay SDK
        // AlipayClient client = ...;
        // AlipayTradePagePayRequest request = ...;
        // return client.pageExecute(request).getBody();
        log.warn("Alipay SDK 未集成，使用模拟支付。orderNo={}", orderNo);
        return generateMockPayForm(orderNo, subject, amount);
    }

    /**
     * 验证异步通知签名
     */
    public boolean verifyNotify(Map<String, String> params) {
        if (!alipayConfig.isConfigured()) {
            log.warn("支付宝未配置，跳过签名验证");
            return "TRADE_SUCCESS".equals(params.get("trade_status"));
        }
        // TODO: 集成真实签名验证
        // return AlipaySignature.rsaCheckV1(params, ...);
        return true;
    }

    /**
     * 验证同步回调签名
     */
    public boolean verifyCallback(Map<String, String> params) {
        return verifyNotify(params);
    }

    /**
     * 生成模拟支付表单
     */
    private String generateMockPayForm(String orderNo, String subject, String amount) {
        return String.format("""
                <form id="pay-form" action="/api/payment/callback" method="get" style="padding:30px;text-align:center;font-family:sans-serif;">
                    <h2>沙箱模拟支付</h2>
                    <p>订单号: %s</p>
                    <p>商品: %s</p>
                    <p style="font-size:24px;color:#f60;">金额: ¥%s</p>
                    <input type="hidden" name="out_trade_no" value="%s"/>
                    <input type="hidden" name="trade_no" value="SIM%s"/>
                    <input type="hidden" name="trade_status" value="TRADE_SUCCESS"/>
                    <button type="submit" style="padding:12px 40px;background:#1677ff;color:#fff;border:none;border-radius:6px;font-size:16px;cursor:pointer;">
                        确认支付 ¥%s
                    </button>
                    <p style="color:#999;margin-top:12px;">* 这是模拟支付页面，实际部署时替换为支付宝沙箱支付</p>
                </form>
                """, orderNo, subject, amount, orderNo, System.currentTimeMillis(), amount);
    }
}
