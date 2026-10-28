package com.example.demo.service;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // SET
    public void set(String key, Object value, long seconds) {
        redisTemplate.opsForValue().set(
                key,
                value,
                Duration.ofSeconds(seconds));
    }

    // GET
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // DELETE
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    // TTL
    public Long getTtl(String key) {
        return redisTemplate.getExpire(key);
    }
}