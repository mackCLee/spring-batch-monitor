package com.lch275.springBatchMonitor.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final StringRedisTemplate redisTemplate;
    private static final String LOCK_VALUE = "LOCKED";

    public boolean acquireLock(String key) {
        return acquireLock(key, 24 * 60 * 60, 60 * 60 * 1000);
    }

    public void releaseLock(String key) {
        redisTemplate.delete(key);
    }
    public boolean acquireLock(String key, long expireTime, long timeout) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeout) {
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(key, LOCK_VALUE, Duration.ofSeconds(expireTime));

            if (Boolean.TRUE.equals(success)) {
                return true; // 락 획득 성공
            }

            // 락 획득 실패 시 짧은 시간 대기 후 재시도 (스핀락)
            try {
                Thread.sleep(100); // 100ms 대기 후 재시도
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false; // 락 획득 실패
    }
}
