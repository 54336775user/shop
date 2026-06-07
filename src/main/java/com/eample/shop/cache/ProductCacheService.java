package com.eample.shop.cache;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Service
public class ProductCacheService {

    private static final String PREFIX = "shop:product:list:";
    private static final long DEFAULT_TTL_SECONDS = 300; // 5 分钟

    private final RedisTemplate<String, Object> redisTemplate;

    public ProductCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 拼商品搜索缓存 key
     * 例如：shop:product:list:数码:1:0:1:8
     */
    public String buildKey(String keyword, Long categoryId, Boolean flashSale, int page, int size) {
        String kw = (keyword == null || keyword.isBlank()) ? "_" : keyword.trim();
        String cid = categoryId == null ? "0" : String.valueOf(categoryId);
        String flash = Boolean.TRUE.equals(flashSale) ? "1" : "0";
        return PREFIX + kw + ":" + cid + ":" + flash + ":" + page + ":" + size;
    }

    /**
     * 读缓存
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getCachedList(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        return (List<T>) value;
    }

    /**
     * 写缓存，默认 5 分钟过期
     */
    public void setCachedList(String key, Object value) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(DEFAULT_TTL_SECONDS));
    }

    /**
     * 删除所有商品列表缓存
     * 学习阶段先用这种简单方式
     */
    public void clearAllProductListCache() {
        Set<String> keys = redisTemplate.keys(PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
