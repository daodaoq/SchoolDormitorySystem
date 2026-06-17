package org.java.backed.ai.circuit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.ai.config.AiProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 模型健康状态监控器（熔断器）
 * 实现 CLOSED → OPEN → HALF_OPEN 三态熔断模式
 * 参考 ragent 项目的 ModelHealthStore 设计
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelHealthMonitor {

    private final AiProperties aiProperties;

    private final Map<String, ModelHealth> healthById = new ConcurrentHashMap<>();

    /**
     * 检查模型是否完全不可用（熔断打开中）
     */
    public boolean isUnavailable(String modelId) {
        ModelHealth health = healthById.get(modelId);
        if (health == null) {
            return false;
        }
        if (health.state == State.OPEN && health.openUntil > System.currentTimeMillis()) {
            return true;
        }
        return health.state == State.HALF_OPEN && health.halfOpenInFlight;
    }

    /**
     * 尝试获取调用许可
     * @return true 表示允许调用，false 表示被熔断拒绝
     */
    public boolean allowCall(String modelId) {
        if (modelId == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        AtomicBoolean allowed = new AtomicBoolean(false);
        healthById.compute(modelId, (k, v) -> {
            if (v == null) {
                v = new ModelHealth();
            }
            if (v.state == State.OPEN) {
                if (v.openUntil > now) {
                    return v; // 熔断期未过，拒绝
                }
                // 熔断期已过，进入半开状态，放行一个探测请求
                v.state = State.HALF_OPEN;
                v.halfOpenInFlight = true;
                allowed.set(true);
                return v;
            }
            if (v.state == State.HALF_OPEN) {
                if (v.halfOpenInFlight) {
                    return v; // 已有探测在进行，拒绝
                }
                v.halfOpenInFlight = true;
                allowed.set(true);
                return v;
            }
            // CLOSED 状态，正常放行
            allowed.set(true);
            return v;
        });
        return allowed.get();
    }

    /**
     * 标记调用成功，重置为健康状态
     */
    public void markSuccess(String modelId) {
        if (modelId == null) {
            return;
        }
        healthById.compute(modelId, (k, v) -> {
            if (v == null) {
                return new ModelHealth();
            }
            v.state = State.CLOSED;
            v.consecutiveFailures = 0;
            v.openUntil = 0L;
            v.halfOpenInFlight = false;
            return v;
        });
        log.debug("模型 {} 熔断器状态: CLOSED（调用成功）", modelId);
    }

    /**
     * 标记调用失败，累加失败计数，达到阈值触发熔断
     */
    public void markFailure(String modelId) {
        if (modelId == null) {
            return;
        }
        long now = System.currentTimeMillis();
        healthById.compute(modelId, (k, v) -> {
            if (v == null) {
                v = new ModelHealth();
            }
            if (v.state == State.HALF_OPEN) {
                // 半开状态探测失败，重新打开熔断
                v.state = State.OPEN;
                v.openUntil = now + aiProperties.getCircuitBreaker().getOpenDurationMs();
                v.consecutiveFailures = 0;
                v.halfOpenInFlight = false;
                log.warn("模型 {} 熔断器状态: HALF_OPEN → OPEN（探测失败），持续 {}ms",
                        modelId, aiProperties.getCircuitBreaker().getOpenDurationMs());
                return v;
            }
            v.consecutiveFailures++;
            if (v.consecutiveFailures >= aiProperties.getCircuitBreaker().getFailureThreshold()) {
                v.state = State.OPEN;
                v.openUntil = now + aiProperties.getCircuitBreaker().getOpenDurationMs();
                v.consecutiveFailures = 0;
                log.warn("模型 {} 熔断器状态: CLOSED → OPEN（连续失败 {} 次），持续 {}ms",
                        modelId, aiProperties.getCircuitBreaker().getFailureThreshold(),
                        aiProperties.getCircuitBreaker().getOpenDurationMs());
            }
            return v;
        });
    }

    /**
     * 获取模型当前健康状态摘要
     */
    public Map<String, String> getHealthStatus() {
        Map<String, String> status = new java.util.LinkedHashMap<>();
        healthById.forEach((id, health) -> status.put(id, health.state.name()));
        return status;
    }

    // ============ 内部类 ============

    private static class ModelHealth {
        private int consecutiveFailures;
        private long openUntil;
        private boolean halfOpenInFlight;
        private State state;

        private ModelHealth() {
            this.consecutiveFailures = 0;
            this.openUntil = 0L;
            this.halfOpenInFlight = false;
            this.state = State.CLOSED;
        }
    }

    private enum State {
        /** 关闭（正常，允许所有调用） */
        CLOSED,
        /** 打开（熔断中，拒绝所有调用） */
        OPEN,
        /** 半开（冷却期结束，允许一个探测请求） */
        HALF_OPEN
    }
}
