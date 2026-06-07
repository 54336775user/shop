package com.eample.shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        // Spring Data Redis 4 新类（不是 GenericJackson2JsonRedisSerializer）
        GenericJacksonJsonRedisSerializer jsonSerializer =
                GenericJacksonJsonRedisSerializer.builder()
                        .build();

        RedisSerializer<String> keySerializer = new StringRedisSerializer();

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(keySerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    //Lua脚本，给token的
    @Bean
    public org.springframework.data.redis.core.script.DefaultRedisScript<Long> orderTokenScript() {
        org.springframework.data.redis.core.script.DefaultRedisScript<Long> script =
                new org.springframework.data.redis.core.script.DefaultRedisScript<>();
        script.setScriptText("""
            local key = KEYS[1]
            if redis.call('get', key) then
                redis.call('del', key)
                return 1
            end
            return 0
            """);
        script.setResultType(Long.class);
        return script;
    }
    @Bean
    public DefaultRedisScript<Long> seckillLockReleaseScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText("""
        local key = KEYS[1]
        local expected = ARGV[1]
        if redis.call('get', key) == expected then
            redis.call('del', key)
            return 1
        end
        return 0
        """);
        script.setResultType(Long.class);
        return script;
    }
}
