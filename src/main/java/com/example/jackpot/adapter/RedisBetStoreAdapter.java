package com.example.jackpot.adapter;

import com.example.jackpot.model.Bet;
import com.example.jackpot.port.BetStore;
import com.example.jackpot.repository.RedisRepository;
import org.springframework.stereotype.Component;

/**
 * Redis adapter that implements the {@link BetStore} port by delegating to {@link RedisRepository}.
 */
@Component
public class RedisBetStoreAdapter implements BetStore {

    private final RedisRepository redisRepository;

    public RedisBetStoreAdapter(RedisRepository redisRepository) {
        this.redisRepository = redisRepository;
    }

    @Override
    public void save(Bet bet) {
        redisRepository.saveBet(bet);
    }
}

