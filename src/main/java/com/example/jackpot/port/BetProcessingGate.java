package com.example.jackpot.port;

import java.time.Duration;

/**
 * Outbound port to coordinate idempotent bet processing.
 */
public interface BetProcessingGate {
    boolean isBetProcessed(String betId);
    boolean tryAcquireProcessing(String betId, Duration ttl);
    void markBetProcessed(String betId);
}

