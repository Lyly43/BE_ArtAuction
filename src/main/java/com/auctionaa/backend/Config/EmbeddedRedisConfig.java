package com.auctionaa.backend.Config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.embedded.RedisServer;

import java.io.IOException;

@Configuration
public class EmbeddedRedisConfig {

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() {
        try {
            redisServer = RedisServer.newRedisServer()
                    .port(7000)
                    .setting("maxmemory 128M")
                    .build();

            redisServer.start();
            System.out.println("✅ Embedded Redis started");
        } catch (Exception e) {
            System.out.println("⚠️ Redis đã chạy sẵn → bỏ qua embedded");
        }
    }

    @PreDestroy
    public void stopRedis() throws IOException {
        if (redisServer != null) {
            redisServer.stop();
            System.out.println("🛑 Embedded Redis stopped");
        }
    }

    @Configuration
    public class RedisConfig {

        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);

            // Key: string
            template.setKeySerializer(new StringRedisSerializer());
            template.setHashKeySerializer(new StringRedisSerializer());

            // Value: json
            GenericJackson2JsonRedisSerializer json = new GenericJackson2JsonRedisSerializer();
            template.setValueSerializer(json);
            template.setHashValueSerializer(json);

            template.afterPropertiesSet();
            return template;
        }
    }
}

