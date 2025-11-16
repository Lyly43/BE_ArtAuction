package com.auctionaa.backend.Config;

import com.auctionaa.backend.Jwt.JwtChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setAllowedOrigins("http://localhost:5173", "http://localhost:3000")
                .withSockJS()
                .setSessionCookieNeeded(false) // Không cần session cookie
                .setStreamBytesLimit(128 * 1024) // Giới hạn stream bytes
                .setHttpMessageCacheSize(1000) // Cache size
                .setDisconnectDelay(5000) // Delay disconnect
                .setHeartbeatTime(25000); // Heartbeat time
        registry.addEndpoint("/stomp")
                .setAllowedOriginPatterns("*")
                .setAllowedOrigins("http://localhost:5173", "http://localhost:3000"); // plain WS (Postman)
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor);
        System.out.println("[WS CONFIG] Inbound interceptor attached");
    }

}
