package com.auctionaa.backend.Jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX = "blacklist:";

    // Thêm token vào blacklist (logout)
    public void blacklistToken(String token, long ttlMillis) {
        long ttlSeconds = ttlMillis / 1000;
        redisTemplate.opsForValue().set(PREFIX + token, "true", ttlSeconds, TimeUnit.SECONDS);
    }

    // Kiểm tra token có bị blacklist không
    public boolean isBlacklisted(String token) {
        Boolean exists = redisTemplate.hasKey(PREFIX + token);
        return exists != null && exists;
    }
}
