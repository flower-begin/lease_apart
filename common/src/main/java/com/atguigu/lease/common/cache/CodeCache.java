package com.atguigu.lease.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 验证码（及类似短寿命键值）的统一存取组件。
 * <p>
 * 优先走 Redis；当本机没有启动 Redis 时自动降级到进程内缓存，
 * 这样开发者 clone 下来即使不装 Redis 也能把登录流程跑通。
 * <p>
 * 一旦 Redis 抛过异常就会被标记为不可用，之后只走内存，避免每次请求都等超时。
 */
@Slf4j
@Component
public class CodeCache {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    private final Map<String, Entry> memory = new ConcurrentHashMap<>();

    /** Redis 是否可用。为 false 时只用内存。 */
    private volatile boolean redisAvailable = true;

    private record Entry(String value, long expireAtMillis) {
        boolean expired() {
            return System.currentTimeMillis() > expireAtMillis;
        }
    }

    /**
     * 写入并设置过期时间（秒）。
     */
    public void put(String key, String value, long ttlSeconds) {
        long expireAt = System.currentTimeMillis() + ttlSeconds * 1000L;
        memory.put(key, new Entry(value, expireAt));
        if (redisAvailable && redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
    }

    /**
     * 读取，过期或不存在返回 null。
     */
    public String get(String key) {
        if (redisAvailable && redisTemplate != null) {
            try {
                Object value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    return String.valueOf(value);
                }
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
        purgeExpired();
        Entry entry = memory.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expired()) {
            memory.remove(key);
            return null;
        }
        return entry.value();
    }

    /**
     * 删除（用于验证码一次性使用）。
     */
    public void remove(String key) {
        memory.remove(key);
        if (redisAvailable && redisTemplate != null) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
    }

    private void markRedisDown(Exception e) {
        if (redisAvailable) {
            log.warn("Redis 不可用，验证码改为进程内缓存（重启后失效）：{}", e.getMessage());
            redisAvailable = false;
        }
    }

    private void purgeExpired() {
        if (memory.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, Entry>> it = memory.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().expired()) {
                it.remove();
            }
        }
    }
}
