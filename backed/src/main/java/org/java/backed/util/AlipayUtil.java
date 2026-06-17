package org.java.backed.util;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.config.AlipayConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 支付宝支付工具类（证书模式 · 沙箱环境）
 */
@Slf4j
@Component
public class AlipayUtil {

    @Autowired
    private AlipayConfig alipayConfig;

    @Autowired
    private AlipayClient alipayClient;

    /**
     * 创建电脑网站支付页面（返回支付宝 HTML form）
     */
    public String createPayPage(String orderNo, String subject, String body, String amount) {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(alipayConfig.getNotifyUrl());
        request.setReturnUrl(alipayConfig.getReturnUrl());

        String bizContent = String.format(
                "{\"out_trade_no\":\"%s\",\"total_amount\":\"%.2f\",\"subject\":\"%s\",\"body\":\"%s\",\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}",
                orderNo, Double.parseDouble(amount),
                escapeJson(subject), escapeJson(body)
        );
        request.setBizContent(bizContent);

        // 调试日志：检查参数是否含特殊字符
        log.info("【支付宝请求参数】orderNo={}, subject={}, body={}, amount={}", orderNo, subject, body, amount);
        log.info("【支付宝请求 JSON】{}", bizContent);
        if (bizContent.contains("+")) {
            log.warn("【警告】bizContent 包含 '+' 字符，可能导致 ALI40247！");
        }

        try {
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            if (response.isSuccess()) {
                log.info("支付宝下单成功: orderNo={}", orderNo);
                return response.getBody();
            } else {
                log.error("支付宝下单失败: orderNo={}, code={}, msg={}, subMsg={}",
                        orderNo, response.getCode(), response.getMsg(), response.getSubMsg());
                return null;
            }
        } catch (AlipayApiException e) {
            log.error("支付宝 API 异常: orderNo={}, errMsg={}", orderNo, e.getErrMsg(), e);
            return null;
        }
    }

    /**
     * 验证异步通知签名
     */
    public boolean verifyNotify(Map<String, String> params) {
        try {
            boolean valid = AlipaySignature.rsaCertCheckV1(
                    params,
                    alipayConfig.getResolvedAlipayPublicCertPath(),
                    alipayConfig.getCharset(),
                    alipayConfig.getSignType()
            );
            log.info("异步通知验签: outTradeNo={}, result={}", params.get("out_trade_no"), valid);
            return valid;
        } catch (AlipayApiException e) {
            log.error("验签异常: errMsg={}", e.getErrMsg(), e);
            return false;
        }
    }

    /**
     * 验证同步回调签名（证书模式）
     */
    public boolean verifyCallback(Map<String, String> params) {
        try {
            boolean valid = AlipaySignature.rsaCertCheckV1(
                    params,
                    alipayConfig.getResolvedAlipayPublicCertPath(),
                    alipayConfig.getCharset(),
                    alipayConfig.getSignType()
            );
            log.info("同步回调验签: outTradeNo={}, result={}", params.get("out_trade_no"), valid);
            return valid;
        } catch (AlipayApiException e) {
            log.error("验签异常: errMsg={}", e.getErrMsg(), e);
            return false;
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
