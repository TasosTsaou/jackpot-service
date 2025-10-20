package com.example.jackpot.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Redis configuration defining connection factory and a JSON-serializing template.
 */
@Configuration
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    /**
     * Creates the connection factory so RedisTemplate can reuse a single Lettuce connection pool.
     *
     * @return configured Redis connection factory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Host/port taken from application.yml; works in Docker Compose too
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    /**
     * Configures RedisTemplate with a Jackson serializer that understands Java time types and
     * preserves polymorphic payloads, matching the data stored by the service.
     *
     * @param cf connection factory used to access Redis
     * @return RedisTemplate ready for application repositories
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);

        // Configure Jackson serializer for values
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.registerModule(new JavaTimeModule()); // add support for java.time types

        // Enable safe default typing for polymorphic (if you ever store interfaces)
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        Jackson2JsonRedisSerializer<Object> valueSerializer =
                new Jackson2JsonRedisSerializer<>(mapper, Object.class);

        StringRedisSerializer keySerializer = new StringRedisSerializer();

        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
