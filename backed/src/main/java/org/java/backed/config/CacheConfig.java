package org.java.backed.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Cache + Redis 配置
 * 统一管理各模块缓存 TTL
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** 收费项目缓存 — 10 分钟（不常变） */
    public static final String CACHE_FEE_ITEMS = "fee_items";
    /** 宿舍信息缓存 — 10 分钟 */
    public static final String CACHE_DORMITORIES = "dormitories";
    /** 菜单缓存 — 30 分钟（很少变） */
    public static final String CACHE_MENUS = "menus";
    /** 学生列表缓存 — 5 分钟 */
    public static final String CACHE_STUDENTS = "students";
    /** 仪表盘统计 — 5 分钟 */
    public static final String CACHE_STATS = "stats";
    /** 短时查询缓存 — 1 分钟 */
    public static final String CACHE_SHORT = "short_term";
    /** 账单查询 — 2 分钟 */
    public static final String CACHE_BILLS = "bills";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        // GenericJackson2JsonRedisSerializer 保留类型信息（@class），
        // 避免反序列化时 LinkedHashMap 无法转为实体类
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // 开启默认类型推断，JSON 中嵌入 @class 字段
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
                ObjectMapper.DefaultTyping.NON_FINAL);
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> ttlMap = new HashMap<>();
        ttlMap.put(CACHE_FEE_ITEMS,   defaultConfig.entryTtl(Duration.ofMinutes(10)));
        ttlMap.put(CACHE_DORMITORIES, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        ttlMap.put(CACHE_MENUS,       defaultConfig.entryTtl(Duration.ofMinutes(30)));
        ttlMap.put(CACHE_STUDENTS,    defaultConfig.entryTtl(Duration.ofMinutes(5)));
        ttlMap.put(CACHE_STATS,       defaultConfig.entryTtl(Duration.ofMinutes(5)));
        ttlMap.put(CACHE_SHORT,       defaultConfig.entryTtl(Duration.ofMinutes(1)));
        ttlMap.put(CACHE_BILLS,       defaultConfig.entryTtl(Duration.ofMinutes(2)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(ttlMap)
                .build();
    }
}
