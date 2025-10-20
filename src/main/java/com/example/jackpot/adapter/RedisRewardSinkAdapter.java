package com.example.jackpot.adapter;

import com.example.jackpot.model.Reward;
import com.example.jackpot.port.RewardSink;
import com.example.jackpot.repository.RedisRepository;
import org.springframework.stereotype.Component;

/**
 * Redis-backed implementation of the {@link RewardSink} port that appends reward
 * records to Redis for auditing and retrieval.
 */
@Component
public class RedisRewardSinkAdapter implements RewardSink {

    private final RedisRepository redisRepository;

    public RedisRewardSinkAdapter(RedisRepository redisRepository) {
        this.redisRepository = redisRepository;
    }

    @Override
    public void appendReward(Reward reward) {
        redisRepository.appendReward(reward);
    }
}
