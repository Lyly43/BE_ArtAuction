package com.auctionaa.backend.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebSocketCorsConfig {

    @Bean
    public CorsFilter webSocketCorsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Cấu hình CORS cho WebSocket
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);
        
        // Thêm headers đặc biệt cho SockJS
        config.addExposedHeader("Access-Control-Allow-Credentials");
        config.addExposedHeader("Access-Control-Allow-Origin");
        config.addExposedHeader("Access-Control-Allow-Methods");
        config.addExposedHeader("Access-Control-Allow-Headers");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        
        // Cấu hình cho tất cả WebSocket endpoints
        source.registerCorsConfiguration("/ws/**", config);
        source.registerCorsConfiguration("/stomp/**", config);
        source.registerCorsConfiguration("/ws/info", config);
        source.registerCorsConfiguration("/ws/info/**", config);
        
        return new CorsFilter(source);
    }
}

