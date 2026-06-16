package org.java.backed.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * 邮件发送工具类
 */
@Slf4j
@Component
public class EmailUtil {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    /**
     * 发送HTML邮件
     */
    public boolean sendHtmlMail(String to, String subject, String htmlContent) {
        if (mailSender == null) {
            log.warn("邮件服务未配置，跳过发送: to={}, subject={}", to, subject);
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom("noreply@example.com");
            mailSender.send(message);
            log.info("邮件发送成功: to={}, subject={}", to, subject);
            return true;
        } catch (MessagingException e) {
            log.error("邮件发送失败: to={}, subject={}", to, subject, e);
            return false;
        }
    }

    /**
     * 发送逾期通知邮件
     */
    public boolean sendOverdueNotice(String to, String studentName, String billNo, String amount, String dueDate) {
        String subject = "【缴费提醒】您有宿舍费用已逾期，请尽快缴费";
        String content = String.format("""
                <h3>逾期缴费提醒</h3>
                <p>亲爱的 %s 同学：</p>
                <p>您的以下账单已逾期未缴，请尽快完成缴费：</p>
                <table border="1" cellpadding="8" cellspacing="0" style="border-collapse:collapse;">
                    <tr><td>账单编号</td><td>%s</td></tr>
                    <tr><td>应缴金额</td><td>¥%s</td></tr>
                    <tr><td>截止日期</td><td>%s</td></tr>
                </table>
                <p>请登录系统查看详情并完成缴费。</p>
                <p>学生宿舍管理系统</p>
                """, studentName, billNo, amount, dueDate);
        return sendHtmlMail(to, subject, content);
    }
}
