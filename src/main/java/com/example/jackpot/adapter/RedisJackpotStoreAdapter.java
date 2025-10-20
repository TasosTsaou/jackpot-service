package com.example.jackpot.adapter;

import com.example.jackpot.model.Jackpot;
import com.example.jackpot.port.JackpotStore;
import com.example.jackpot.repository.RedisRepository;
import org.springframework.stereotype.Component;

/**
 * Redis-backed implementation of the {@link JackpotStore} port for persisting
 * jackpot snapshots and pool amounts.
 */
@Component
public class RedisJackpotStoreAdapter implements JackpotStore {

    private final RedisRepository redisRepository;

    public RedisJackpotStoreAdapter(RedisRepository redisRepository) {
        this.redisRepository = redisRepository;
    }

    @Override
    public void saveJackpot(Jackpot jackpot) {
        redisRepository.saveJackpot(jackpot);
    }
}
