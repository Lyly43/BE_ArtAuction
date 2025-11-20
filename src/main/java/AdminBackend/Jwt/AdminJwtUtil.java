package AdminBackend.Jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class AdminJwtUtil {

    @Value("${app.jwt.admin-secret:${app.jwt.secret}}")
    private String adminSecret;

    private Key key;

    private static final long ADMIN_EXPIRATION_TIME = 1000L * 60 * 60 * 24 * 7; // 7 ngày

    @PostConstruct
    public void init() {
        if (adminSecret == null || adminSecret.isEmpty()) {
            throw new IllegalStateException("Admin JWT secret is required. Set app.jwt.admin-secret or app.jwt.secret in application.properties");
        }

        if (adminSecret.length() < 32) {
            throw new IllegalStateException("Admin JWT secret must be at least 32 characters for HS256");
        }

        this.key = Keys.hmacShaKeyFor(adminSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAdminToken(String adminId, String role) {
        return Jwts.builder()
                .setSubject(adminId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ADMIN_EXPIRATION_TIME))
                .claim("role", role == null ? "4" : role)
                .claim("tokenType", "ADMIN")
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractAdminId(String headerOrToken) {
        String token = sanitize(headerOrToken);
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String extractRole(String headerOrToken) {
        String token = sanitize(headerOrToken);
        return (String) Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role");
    }

    public boolean validateToken(String headerOrToken) {
        try {
            String token = sanitize(headerOrToken);
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private String sanitize(String headerOrToken) {
        if (headerOrToken == null) return "";
        String t = headerOrToken.trim();
        if (t.startsWith("Bearer ")) t = t.substring(7);
        return t.trim();
    }
}

