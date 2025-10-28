package com.example.jackpot.adapter;

import com.example.jackpot.port.BetProcessingGate;
import com.example.jackpot.repository.RedisRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class RedisBetProcessingGateAdapter implements BetProcessingGate {
    private final RedisRepository redis;

    public RedisBetProcessingGateAdapter(RedisRepository redis) {
        this.redis = redis;
    }

    @Override
    public boolean isBetProcessed(String betId) {
        return redis.isBetProcessed(betId);
    }

    @Override
    public boolean tryAcquireProcessing(String betId, Duration ttl) {
        return redis.tryAcquireBetProcessing(betId, UUID.randomUUID().toString(), ttl);
    }

    @Override
    public void markBetProcessed(String betId) {
        redis.markBetProcessed(betId);
    }
}

