package com.example.jackpot.port;

import com.example.jackpot.model.Bet;

/**
 * Outbound port for persisting bets.
 * Implementations adapt the underlying storage (e.g., Redis) behind this interface.
 */
public interface BetStore {
    /**
     * Persist the provided bet for later retrieval.
     *
     * @param bet bet payload to save
     */
    void save(Bet bet);
}

