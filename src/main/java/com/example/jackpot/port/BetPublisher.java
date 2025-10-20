package com.example.jackpot.port;

import com.example.jackpot.model.Bet;

/**
 * Outbound port for publishing bets to a messaging system.
 * Implementations adapt technology details (e.g., Kafka) behind this interface.
 */
public interface BetPublisher {
    /**
     * Publish the given bet event.
     *
     * @param bet bet payload to publish
     */
    void publish(Bet bet);
}

