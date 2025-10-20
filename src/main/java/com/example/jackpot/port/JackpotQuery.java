package com.example.jackpot.port;

import com.example.jackpot.model.Jackpot;

import java.util.Optional;

/**
 * Outbound port for reading jackpots by id.
 */
public interface JackpotQuery {
    Optional<Jackpot> findJackpotById(String id);
}

