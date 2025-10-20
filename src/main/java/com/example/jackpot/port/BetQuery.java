package com.example.jackpot.port;

import com.example.jackpot.model.Bet;

import java.util.Optional;

/**
 * Outbound port for reading bet records by id.
 */
public interface BetQuery {
    Optional<Bet> findBet(String betId);
}

