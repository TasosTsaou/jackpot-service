package com.example.jackpot.port;

/**
 * Outbound port for atomic jackpot pool adjustments.
 */
public interface JackpotPool {
    /**
     * Atomically adds delta to the jackpot pool and returns the new value.
     */
    double incrementPool(String jackpotId, double delta);
}

