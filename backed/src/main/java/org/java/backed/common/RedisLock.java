package org.java.backed.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁工具
 */
@Slf4j
public class RedisLock {

    private static final String LOCK_PREFIX = "lock:";
    private static final long DEFAULT_WAIT_MS = 3000;
    private static final long DEFAULT_LEASE_MS = 10000;

    // Lua脚本: 释放锁(原子操作)
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

    private final StringRedisTemplate redisTemplate;
    private final String lockKey;
    private final String lockValue;
    private final long leaseMs;

    public RedisLock(StringRedisTemplate redisTemplate, String lockName, long leaseMs) {
        this.redisTemplate = redisTemplate;
        this.lockKey = LOCK_PREFIX + lockName;
        this.lockValue = UUID.randomUUID().toString();
        this.leaseMs = leaseMs > 0 ? leaseMs : DEFAULT_LEASE_MS;
    }

    public RedisLock(StringRedisTemplate redisTemplate, String lockName) {
        this(redisTemplate, lockName, DEFAULT_LEASE_MS);
    }

    /**
     * 尝试获取锁（非阻塞）
     */
    public boolean tryLock() {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, leaseMs, TimeUnit.MILLISECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 获取锁（阻塞等待）
     */
    public boolean lock() {
        return lock(DEFAULT_WAIT_MS);
    }

    public boolean lock(long waitMs) {
        long start = System.currentTimeMillis();
        while (!tryLock()) {
            if (System.currentTimeMillis() - start > waitMs) {
                log.warn("获取锁超时: {}", lockKey);
                return false;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    /**
     * 释放锁
     */
    public void unlock() {
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script, Collections.singletonList(lockKey), lockValue);
            if (result != null && result == 0) {
                log.debug("锁已过期或不属于当前线程: {}", lockKey);
            }
        } catch (Exception e) {
            log.error("释放锁异常: {}", lockKey, e);
        }
    }
}
