package com.renauldiedev.event.infrastructure.redis;

import com.renauldiedev.event.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class IdempotencyService {

    private static final String IDEMPOTENCY_PREFIX = "idempotency:";

    private final RedisTemplate<String, Object> redisTemplate;

    public IdempotencyService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean acquire(String key, Duration ttl) {
        String redisKey = IDEMPOTENCY_PREFIX + key;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSING", ttl);
        return Boolean.TRUE.equals(acquired);
    }

    public void validateUniqueRequest(String key, Duration ttl) {
        if (!acquire(key, ttl)) {
            log.warn("Duplicate request detected for idempotency key: {}", key);
            throw new BadRequestException("Duplicate request detected. This operation is already being processed or completed.");
        }
    }

    public void complete(String key, Duration ttl) {
        String redisKey = IDEMPOTENCY_PREFIX + key;
        redisTemplate.opsForValue().set(redisKey, "COMPLETED", ttl);
    }

    public void release(String key) {
        String redisKey = IDEMPOTENCY_PREFIX + key;
        redisTemplate.delete(redisKey);
    }
}
