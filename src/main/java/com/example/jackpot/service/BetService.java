package com.example.jackpot.service;

import com.example.jackpot.model.Bet;
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

    public BetService(BetStore betStore, BetPublisher betPublisher) {
        this.betStore = betStore;
        this.betPublisher = betPublisher;
    }

    /**
     * Persist the bet and publish it via the outbound publisher port.
     */
    public void handleBet(Bet bet) {
        betStore.save(bet);
        betPublisher.publish(bet);
    }
}
