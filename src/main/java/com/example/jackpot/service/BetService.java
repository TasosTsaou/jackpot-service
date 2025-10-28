package com.example.jackpot.service;

import com.example.jackpot.model.Bet;
import com.example.jackpot.port.BetProcessingGate;
import com.example.jackpot.port.BetPublisher;
import com.example.jackpot.port.BetStore;
import org.springframework.stereotype.Service;

/**
 * Application service that persists incoming bets and publishes them through
 * the outbound messaging port.
 */
@Service
public class BetService {

    private final BetStore betStore;
    private final BetPublisher betPublisher;
    private final BetProcessingGate processingGate;

    public BetService(BetStore betStore, BetPublisher betPublisher, BetProcessingGate processingGate) {
        this.betStore = betStore;
        this.betPublisher = betPublisher;
        this.processingGate = processingGate;
    }

    /**
     * Persist the bet and publish it via the outbound publisher port.
     */
    public void handleBet(Bet bet) {
        // If this bet has already been fully processed, accept idempotently without republishing
        if (processingGate.isBetProcessed(bet.getBetId())) {
            return;
        }
        betStore.save(bet);
        betPublisher.publish(bet);
    }
}
