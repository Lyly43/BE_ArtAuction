package com.auctionaa.backend.Config;

import AdminBackend.Jwt.AdminJwtAuthFilter;
import com.auctionaa.backend.Jwt.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // JWT filter của bạn (nếu chưa có bean, tạo @Component cho JwtAuthFilter)
    private final JwtAuthFilter jwtAuthFilter;
    private final AdminJwtAuthFilter adminJwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // REST + JWT: tắt CSRF toàn cục (đơn giản, dễ test Postman)
                .csrf(csrf -> csrf.disable())
                // CORS cho FE (localhost:5173) - sử dụng custom CORS filter
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // JWT => stateless; tắt formLogin/httpBasic
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())

                // Phân quyền
                .authorizeHttpRequests(auth -> auth
                        // Swagger/OpenAPI endpoints - cho phép truy cập công khai để xem tài liệu API
                        // Truy cập Swagger UI tại: http://localhost:8081/swagger-ui.html
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()

                        // WebSocket/SockJS - cần permitAll để SockJS có thể kết nối
                        .requestMatchers("/ws/**", "/stomp/**", "/ws/info", "/ws/info/**", "/ws/websocket").permitAll()

                        .requestMatchers(HttpMethod.GET, " /api/all-seller-requests").permitAll()
                        // Auth (public)
                        .requestMatchers(HttpMethod.POST, "/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auctionroom/search-public").permitAll()
                        .requestMatchers(HttpMethod.POST, "/verify-otp").permitAll()
                        .requestMatchers(HttpMethod.POST, "/resend-otp").permitAll()

                        // Admin Auth (public - cho phép login)
                        .requestMatchers(HttpMethod.POST, "/api/admin/auth/login").permitAll()

                        // Stream (theo cấu hình bạn đang test)
                        // - startStream: bạn tự validate token trong controller => permitAll để vào
                        // controller
                        .requestMatchers(HttpMethod.POST, "/api/stream/**").permitAll()
                        // - getRoom public để người xem lấy thông tin phòng
                        .requestMatchers(HttpMethod.GET, "/api/stream/room/**").permitAll()
                        // - token endpoint: tự validate token trong controller (cho phép cả user và admin)
                        .requestMatchers(HttpMethod.GET, "/api/stream/token").permitAll()

                        // Chat endpoints - cần authentication
                        .requestMatchers(HttpMethod.GET, "/api/chats/**").authenticated()

                        // Ví dụ endpoint topup mở public (giữ nguyên từ config cũ)
                        .requestMatchers(HttpMethod.POST, "/api/wallets/topups").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/artwork/featured").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/artwork/top-4-highest-price").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auctionroom/*").permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/auctionroom/*/members", "GET")).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auctionroom/members").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auctionroom/top-4-ongoing").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auctionroom/top-4-upcoming-highest-price").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/artwork/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/artwork/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/wallets/{id}/verify-capture").permitAll()
                        .requestMatchers(HttpMethod.POST, "/register", "/api/auth/register").permitAll()

                        // Search endpoints - public (POST với JSON body)
                        .requestMatchers(HttpMethod.POST, "/api/auctionroom/search").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/artwork/search").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/invoice/search").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/wallets/search").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/history/search").permitAll()
                        // Cho phép /error để tránh 404 -> 403
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/user/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/user/profile", "/api/user/profile/avatar")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/user/info").authenticated()
                        // Preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/auctionroom/*").permitAll()
                        // Còn lại: yêu cầu đã đăng nhập (JWT)
                        .anyRequest().authenticated())

                // Đưa JWT filter vào trước UsernamePasswordAuthenticationFilter
                // Admin filter trước để xử lý admin token, sau đó mới đến user filter
                .addFilterBefore(adminJwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS cho FE (Vite: http://localhost:5173)
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*")); // hoặc liệt kê: Authorization, Content-Type, ...
        config.setAllowCredentials(true); // QUAN TRỌNG: Phải là true cho WebSocket
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        // Cấu hình đặc biệt cho WebSocket endpoints
        CorsConfiguration wsConfig = new CorsConfiguration();
        wsConfig.setAllowedOriginPatterns(List.of("*"));
        wsConfig.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
        wsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        wsConfig.setAllowedHeaders(List.of("*"));
        wsConfig.setAllowCredentials(true);
        wsConfig.setMaxAge(3600L);

        source.registerCorsConfiguration("/ws/**", wsConfig);
        source.registerCorsConfiguration("/stomp/**", wsConfig);
        source.registerCorsConfiguration("/ws/info/**", wsConfig);

        // Cấu hình đặc biệt cho SockJS info endpoint
        CorsConfiguration sockjsConfig = new CorsConfiguration();
        sockjsConfig.setAllowedOriginPatterns(List.of("*"));
        sockjsConfig.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
        sockjsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        sockjsConfig.setAllowedHeaders(List.of("*"));
        sockjsConfig.setAllowCredentials(true);
        sockjsConfig.setMaxAge(3600L);

        // Thêm exposed headers cho SockJS
        sockjsConfig.addExposedHeader("Access-Control-Allow-Credentials");
        sockjsConfig.addExposedHeader("Access-Control-Allow-Origin");
        sockjsConfig.addExposedHeader("Access-Control-Allow-Methods");
        sockjsConfig.addExposedHeader("Access-Control-Allow-Headers");

        source.registerCorsConfiguration("/ws/info", sockjsConfig);
        source.registerCorsConfiguration("/ws/info/**", sockjsConfig);

        return source;
    }

}
