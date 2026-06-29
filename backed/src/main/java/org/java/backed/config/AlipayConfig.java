package org.java.backed.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;

/**
 * 支付宝沙箱配置（证书模式）
 * <p>
 * 使用支付宝公钥证书方式签名，需要三份证书：
 * - appPublicCert.crt   应用公钥证书
 * - alipayPublicCert.crt 支付宝公钥证书
 * - alipayRootCert.crt  支付宝根证书
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {

    private String appId;
    private String privateKey;
    private String appCertPath;
    private String alipayPublicCertPath;
    private String rootCertPath;
    private String gatewayUrl;
    private String notifyUrl;
    private String returnUrl;
    private String charset = "UTF-8";
    private String format = "json";
    private String signType = "RSA2";

    /** 解析后的文件系统绝对路径 */
    private String resolvedAppCertPath;
    private String resolvedAlipayPublicCertPath;
    private String resolvedRootCertPath;

    @PostConstruct
    public void resolvePaths() {
        this.resolvedAppCertPath = resolveClasspath(appCertPath);
        this.resolvedAlipayPublicCertPath = resolveClasspath(alipayPublicCertPath);
        this.resolvedRootCertPath = resolveClasspath(rootCertPath);
        log.info("证书路径解析完成: appCert={}, alipayCert={}, rootCert={}",
                resolvedAppCertPath, resolvedAlipayPublicCertPath, resolvedRootCertPath);
    }

    /**
     * 解析 classpath 资源为文件系统绝对路径（Alipay SDK 需要）
     * 兼容 JAR 内运行 —— 资源会复制到临时文件
     */
    private String resolveClasspath(String path) {
        if (path == null) return null;
        try {
            ClassPathResource resource = new ClassPathResource(path);
            try {
                // 优先尝试直接文件（IDE 开发模式）
                File file = resource.getFile();
                return file.getAbsolutePath();
            } catch (IOException e) {
                // JAR 内运行，复制到临时文件
                String suffix = path.contains(".") ? path.substring(path.lastIndexOf('.')) : ".tmp";
                java.io.File tempFile = java.io.File.createTempFile("alipay-cert-", suffix);
                tempFile.deleteOnExit();
                try (java.io.InputStream is = resource.getInputStream();
                     java.io.FileOutputStream os = new java.io.FileOutputStream(tempFile)) {
                    is.transferTo(os);
                }
                log.info("证书从 classpath 复制到临时文件: {} -> {}", path, tempFile.getAbsolutePath());
                return tempFile.getAbsolutePath();
            }
        } catch (IOException e) {
            log.error("解析证书路径失败: {}", path, e);
            return path; // fallback
        }
    }

    @Bean
    public AlipayClient alipayClient() {
        com.alipay.api.AlipayConfig sdkConfig = new com.alipay.api.AlipayConfig();
        sdkConfig.setServerUrl(gatewayUrl);
        sdkConfig.setAppId(appId);
        sdkConfig.setPrivateKey(privateKey);
        sdkConfig.setFormat(format);
        sdkConfig.setCharset(charset);
        sdkConfig.setSignType(signType);
        // 证书模式
        sdkConfig.setAppCertPath(resolvedAppCertPath);
        sdkConfig.setAlipayPublicCertPath(resolvedAlipayPublicCertPath);
        sdkConfig.setRootCertPath(resolvedRootCertPath);

        try {
            log.info("支付宝证书模式 AlipayClient 初始化: appId={}", appId);
            return new DefaultAlipayClient(sdkConfig);
        } catch (AlipayApiException e) {
            log.error("AlipayClient 初始化失败: {}", e.getErrMsg(), e);
            throw new RuntimeException("支付宝客户端初始化失败: " + e.getErrMsg(), e);
        }
    }
}
