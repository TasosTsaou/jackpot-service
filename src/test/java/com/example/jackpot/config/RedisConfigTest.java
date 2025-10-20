package com.example.jackpot.config;

import com.example.jackpot.model.Reward;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifies Redis configuration choices so serialization stays aligned with application needs.
 */
class RedisConfigTest {

    private final RedisConfig config = new RedisConfig();

    @Test
    // Ensures the configured serializer can handle java.time.Instant values without failures.
    void redisTemplateSerializesInstantFields() {
        RedisConnectionFactory connectionFactory = Mockito.mock(RedisConnectionFactory.class);

        RedisTemplate<String, Object> template = config.redisTemplate(connectionFactory);
        @SuppressWarnings("unchecked")
        Jackson2JsonRedisSerializer<Object> serializer =
                (Jackson2JsonRedisSerializer<Object>) template.getValueSerializer();

        Reward reward = Reward.builder()
                .betId("bet-1")
                .userId("user-1")
                .jackpotId("J1")
                .jackpotRewardAmount(42.0)
                .createdAt(Instant.now())
                .build();

        assertThatCode(() -> serializer.serialize(reward))
                .doesNotThrowAnyException();
    }
}
