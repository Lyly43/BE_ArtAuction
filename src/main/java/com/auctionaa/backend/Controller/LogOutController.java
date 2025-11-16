package com.auctionaa.backend.Controller;

import com.auctionaa.backend.Jwt.JwtUtil;
import com.auctionaa.backend.Jwt.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LogOutController {
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Missing or invalid token");
        }

        String token = authHeader.substring(7);
        Date expiry = jwtUtil.getExpirationDate(token);

        long ttl = expiry.getTime() - System.currentTimeMillis();
        if (ttl > 0) {
            blacklistService.blacklistToken(token, ttl);
        }

        return ResponseEntity.ok("Logged out successfully");
    }
}

