package com.example.jackpot.port;

import com.example.jackpot.model.Jackpot;

/**
 * Outbound port for persisting jackpot state.
 */
public interface JackpotStore {
    void saveJackpot(Jackpot jackpot);
}

