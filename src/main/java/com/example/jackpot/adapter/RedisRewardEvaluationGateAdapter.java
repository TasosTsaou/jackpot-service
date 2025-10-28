package com.example.jackpot.adapter;

import com.example.jackpot.model.Reward;
import com.example.jackpot.port.RewardEvaluationGate;
import com.example.jackpot.repository.RedisRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisRewardEvaluationGateAdapter implements RewardEvaluationGate {
    private final RedisRepository redis;

    public RedisRewardEvaluationGateAdapter(RedisRepository redis) {
        this.redis = redis;
    }

    @Override
    public Optional<Reward> findRewardByBetId(String betId) {
        return redis.findRewardByBetId(betId);
    }

    @Override
    public void saveRewardByBetId(Reward reward) {
        redis.saveRewardByBetId(reward);
    }

    @Override
    public boolean tryAcquireRewardLock(String betId, Duration ttl) {
        return redis.tryAcquireRewardLock(betId, "reward-eval", ttl);
    }
}

