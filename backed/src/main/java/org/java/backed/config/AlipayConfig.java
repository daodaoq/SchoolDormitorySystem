package org.java.backed.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝沙箱配置 (Alipay SDK stub - 替换为真实SDK后启用)
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {

    private String appId;
    private String privateKey;
    private String alipayPublicKey;
    private String gatewayUrl;
    private String notifyUrl;
    private String returnUrl;
    private String charset = "UTF-8";
    private String format = "json";
    private String signType = "RSA2";

    public boolean isConfigured() {
        return appId != null && !appId.isEmpty()
                && !"your-sandbox-app-id".equals(appId);
    }
}
