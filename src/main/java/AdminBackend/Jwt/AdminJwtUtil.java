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

    public String generateAdminToken(String adminId, Integer role) {
        int roleValue = (role != null && role != 0) ? role : 3; // Default role 3 for admin
        return Jwts.builder()
                .setSubject(adminId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ADMIN_EXPIRATION_TIME))
                .claim("role", roleValue)
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
        Object roleObj = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role");
        
        // Xử lý cả trường hợp role là Integer hoặc String
        if (roleObj == null) {
            return "3"; // Default role
        }
        if (roleObj instanceof Integer) {
            return String.valueOf((Integer) roleObj);
        }
        if (roleObj instanceof String) {
            return (String) roleObj;
        }
        // Fallback: convert sang String
        return String.valueOf(roleObj);
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

    /**
     * Kiểm tra xem token có phải admin token không dựa vào claim "tokenType"
     */
    public boolean isAdminToken(String headerOrToken) {
        try {
            String token = sanitize(headerOrToken);
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            Object tokenType = claims.get("tokenType");
            return "ADMIN".equals(tokenType);
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

