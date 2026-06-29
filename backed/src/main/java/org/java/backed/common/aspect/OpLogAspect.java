package org.java.backed.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.java.backed.common.annotation.OpLog;
import org.java.backed.entity.SysUser;
import org.java.backed.mapper.SysUserMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.java.backed.config.RabbitMQConfig.EXCHANGE_OPLOG;
import static org.java.backed.config.RabbitMQConfig.RK_OPLOG;

/**
 * 操作日志切面 — 拦截 @OpLog 注解的方法，自动记录审计日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OpLogAspect {

    private final SysUserMapper userMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint jp, OpLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        String status = "SUCCESS";
        String errorMsg = null;
        Object result;

        try {
            result = jp.proceed();
        } catch (Throwable e) {
            status = "FAIL";
            errorMsg = e.getMessage();
            throw e;
        } finally {
            try {
                long duration = System.currentTimeMillis() - start;
                Map<String, Object> logEntry = buildLogEntry(jp, opLog, status, errorMsg, duration);
                // 异步发送到 RabbitMQ，不阻塞业务线程
                rabbitTemplate.convertAndSend(EXCHANGE_OPLOG, RK_OPLOG, logEntry);
            } catch (Exception e) {
                // 日志记录失败不影响业务流程
                log.warn("操作日志发送失败: {}", e.getMessage());
            }
        }

        return result;
    }

    private Map<String, Object> buildLogEntry(ProceedingJoinPoint jp, OpLog opLog,
                                               String status, String errorMsg, long duration) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("createTime", LocalDateTime.now().toString());

        // 当前用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            String username = auth.getName();
            entry.put("username", username);
            SysUser user = userMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getUsername, username));
            if (user != null) {
                entry.put("userId", user.getId());
                entry.put("realName", user.getRealName());
            }
        }

        // 元数据
        entry.put("module", opLog.module());
        entry.put("action", opLog.action());
        entry.put("description", opLog.description());
        entry.put("method", jp.getSignature().toShortString());
        entry.put("duration", duration);
        entry.put("status", status);
        if (errorMsg != null) {
            entry.put("errorMsg", errorMsg.length() > 500 ? errorMsg.substring(0, 500) : errorMsg);
        }

        // 请求参数（脱敏：过滤 password 等敏感字段）
        if (opLog.logParams()) {
            try {
                Object[] args = jp.getArgs();
                // 过滤掉 HttpServletRequest / HttpServletResponse 等
                Map<String, Object> safeArgs = new LinkedHashMap<>();
                String[] paramNames = ((MethodSignature) jp.getSignature()).getParameterNames();
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    if (arg instanceof jakarta.servlet.http.HttpServletRequest
                            || arg instanceof jakarta.servlet.http.HttpServletResponse
                            || arg instanceof jakarta.servlet.ServletResponse
                            || arg instanceof jakarta.servlet.ServletRequest) {
                        continue;
                    }
                    String name = paramNames != null && i < paramNames.length ? paramNames[i] : "arg" + i;
                    String json = objectMapper.writeValueAsString(arg);
                    // 脱敏
                    json = json.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"");
                    json = json.replaceAll("\"oldPassword\"\\s*:\\s*\"[^\"]*\"", "\"oldPassword\":\"***\"");
                    if (json.length() > 2000) json = json.substring(0, 2000) + "...";
                    safeArgs.put(name, json);
                }
                entry.put("requestParams", objectMapper.writeValueAsString(safeArgs));
            } catch (Exception ignored) {}
        }

        // 客户端信息
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                jakarta.servlet.http.HttpServletRequest req = attrs.getRequest();
                entry.put("ipAddress", getClientIp(req));
                entry.put("userAgent", req.getHeader("User-Agent"));
            }
        } catch (Exception ignored) {}

        return entry;
    }

    private String getClientIp(jakarta.servlet.http.HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getRemoteAddr();
        }
        return ip != null && ip.length() > 45 ? ip.substring(0, 45) : ip;
    }
}
