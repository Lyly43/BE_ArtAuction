package AdminBackend.Jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminJwtAuthFilter extends OncePerRequestFilter {

    private final AdminJwtUtil adminJwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // Chỉ xử lý các request đến /api/admin/**
        String path = req.getRequestURI();
        if (!path.startsWith("/api/admin/")) {
            chain.doFilter(req, res);
            return;
        }

        // Bỏ qua login endpoint
        if (path.equals("/api/admin/auth/login")) {
            chain.doFilter(req, res);
            return;
        }

        String auth = req.getHeader("Authorization");

        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);

            try {
                if (adminJwtUtil.validateToken(token)) {
                    String adminId = adminJwtUtil.extractAdminId(token);
                    var authToken = new UsernamePasswordAuthenticationToken(adminId, null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                // Token không hợp lệ hoặc không phải admin token
                // Không set authentication, Spring Security sẽ xử lý
            }
        }

        chain.doFilter(req, res);
    }
}



















