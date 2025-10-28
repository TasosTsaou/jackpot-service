package com.example.jackpot.adapter;

import com.example.jackpot.port.JackpotPool;
import com.example.jackpot.repository.RedisRepository;
import org.springframework.stereotype.Component;

@Component
public class RedisJackpotPoolAdapter implements JackpotPool {
    private final RedisRepository redis;

    public RedisJackpotPoolAdapter(RedisRepository redis) {
        this.redis = redis;
    }

    @Override
    public double incrementPool(String jackpotId, double delta) {
        return redis.incrementJackpotPool(jackpotId, delta);
    }
}

