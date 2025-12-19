package com.auctionaa.backend.Otp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class OtpUtil {
    private static final SecureRandom random = new SecureRandom();

    public static String generate6Digits() {
        int n = 100000 + random.nextInt(900000);
        return String.valueOf(n);
    }

    public static String sha256Base64(String raw, String pepper) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((raw + pepper).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}