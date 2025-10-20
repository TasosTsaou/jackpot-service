package com.example.jackpot.adapter;

import com.example.jackpot.model.Jackpot;
import com.example.jackpot.port.JackpotQuery;
import com.example.jackpot.repository.RedisRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Redis-based reader for the {@link JackpotQuery} port to find jackpots by id.
 */
@Component
public class RedisJackpotQueryAdapter implements JackpotQuery {

    private final RedisRepository redisRepository;

    public RedisJackpotQueryAdapter(RedisRepository redisRepository) {
        this.redisRepository = redisRepository;
    }

    @Override
    public Optional<Jackpot> findJackpotById(String id) {
        return redisRepository.findJackpotById(id);
    }
}
