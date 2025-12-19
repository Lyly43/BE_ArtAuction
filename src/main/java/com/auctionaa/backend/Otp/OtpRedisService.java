package com.auctionaa.backend.Otp;

import com.auctionaa.backend.DTO.OtpToRedis.OtpPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpRedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.otp.pepper}") private String pepper;
    @Value("${app.otp.expireMinutes}") private long expireMinutes;
    @Value("${app.otp.maxAttempts}") private int maxAttempts;
    @Value("${app.otp.resendCooldownSeconds}") private long resendCooldownSeconds;

    private String otpKey(String email) { return "otp:email:" + email.toLowerCase(); }
    private String attemptsKey(String email) { return "otp:attempts:" + email.toLowerCase(); }
    private String resendKey(String email) { return "otp:resend_cd:" + email.toLowerCase(); }

    public boolean canResend(String email) {
        // SETNX cooldown key
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(
                resendKey(email),
                "1",
                Duration.ofSeconds(resendCooldownSeconds)
        );
        return Boolean.TRUE.equals(ok);
    }

    public String createAndStoreOtp(String email) {
        String otp = OtpUtil.generate6Digits();
        String hash = OtpUtil.sha256Base64(otp, pepper);

        OtpPayload payload = OtpPayload.builder()
                .otpHash(hash)
                .createdAt(LocalDateTime.now())
                .build();

        redisTemplate.opsForValue().set(otpKey(email), payload, Duration.ofMinutes(expireMinutes));
        // reset attempts
        redisTemplate.delete(attemptsKey(email));

        return otp; // trả OTP plain để gửi mail
    }

    public VerifyResult verify(String email, String otpInput) {
        Object obj = redisTemplate.opsForValue().get(otpKey(email));
        if (obj == null) return VerifyResult.EXPIRED_OR_NOT_FOUND;

        // attempts
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey(email));
        if (attempts != null && attempts > maxAttempts) {
            redisTemplate.delete(otpKey(email));
            return VerifyResult.TOO_MANY_ATTEMPTS;
        }

        OtpPayload payload = (OtpPayload) obj;
        String inputHash = OtpUtil.sha256Base64(otpInput, pepper);

        if (!inputHash.equals(payload.getOtpHash())) {
            return VerifyResult.INVALID;
        }

        // success -> xóa OTP + attempts
        redisTemplate.delete(otpKey(email));
        redisTemplate.delete(attemptsKey(email));
        return VerifyResult.OK;
    }

    public enum VerifyResult {
        OK, INVALID, EXPIRED_OR_NOT_FOUND, TOO_MANY_ATTEMPTS
    }

    public void delete(String email){
        redisTemplate.delete(otpKey(email));
        redisTemplate.delete(attemptsKey(email));
        redisTemplate.delete(resendKey(email));
    }

}
