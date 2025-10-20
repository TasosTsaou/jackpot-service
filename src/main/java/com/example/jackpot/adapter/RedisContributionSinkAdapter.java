package com.example.jackpot.adapter;

import com.example.jackpot.model.Contribution;
import com.example.jackpot.port.ContributionSink;
import com.example.jackpot.repository.RedisRepository;
import org.springframework.stereotype.Component;

/**
 * Redis-backed implementation of the {@link ContributionSink} port that stores
 * contribution records associated with a jackpot.
 */
@Component
public class RedisContributionSinkAdapter implements ContributionSink {

    private final RedisRepository redisRepository;

    public RedisContributionSinkAdapter(RedisRepository redisRepository) {
        this.redisRepository = redisRepository;
    }

    @Override
    public void appendContribution(Contribution contribution) {
        redisRepository.appendContribution(contribution);
    }
}
