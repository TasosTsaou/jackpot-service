package com.example.jackpot.adapter;

import com.example.jackpot.model.Bet;
import com.example.jackpot.port.BetQuery;
import com.example.jackpot.repository.RedisRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Redis-based reader for the {@link BetQuery} port to retrieve bets by id.
 */
@Component
public class RedisBetQueryAdapter implements BetQuery {

    private final RedisRepository redisRepository;

    public RedisBetQueryAdapter(RedisRepository redisRepository) {
        this.redisRepository = redisRepository;
    }

    @Override
    public Optional<Bet> findBet(String betId) {
        return redisRepository.findBet(betId);
    }
}
