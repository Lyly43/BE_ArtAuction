package com.auctionaa.backend.Otp;

import com.auctionaa.backend.DTO.OtpToRedis.PendingRegisterPayload;
import com.auctionaa.backend.DTO.Request.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PendingRegisterRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.register.pendingExpireMinutes:15}")
    private long pendingExpireMinutes;

    private String key(String email) {
        return "pending_register:" + email.toLowerCase();
    }

    public void store(RegisterRequest req, String passwordHash) {
        PendingRegisterPayload payload = PendingRegisterPayload.builder()
                .email(req.getEmail().toLowerCase())
                .username(req.getUsername())
                .passwordHash(passwordHash)
                .phone(req.getPhone())
                .createdAtMillis(System.currentTimeMillis())
                .build();

        redisTemplate.opsForValue().set(key(req.getEmail()), payload, Duration.ofMinutes(pendingExpireMinutes));
    }

    public PendingRegisterPayload get(String email) {
        Object obj = redisTemplate.opsForValue().get(key(email));
        if (obj == null) {
            return null;
        }
        // ✅ Dùng ObjectMapper để convert từ LinkedHashMap/Map sang PendingRegisterPayload
        return objectMapper.convertValue(obj, PendingRegisterPayload.class);
    }

    public void delete(String email) {
        redisTemplate.delete(key(email));
    }
}
