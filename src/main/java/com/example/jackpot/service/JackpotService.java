package com.example.jackpot.service;

import com.example.jackpot.model.*;
import com.example.jackpot.model.enums.ContributionStrategyType;
import com.example.jackpot.port.ContributionSink;
import com.example.jackpot.port.JackpotQuery;
import com.example.jackpot.port.JackpotStore;
import com.example.jackpot.port.BetProcessingGate;
import com.example.jackpot.port.JackpotPool;
import com.example.jackpot.strategy.contribution.ContributionStrategy;
import com.example.jackpot.strategy.registry.ContributionStrategyRegistry;
import com.example.jackpot.repository.RedisRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Application service that processes bets consumed from Kafka, calculates
 * contributions, and updates jackpot state via ports.
 */
@Service
public class JackpotService {

    private final JackpotQuery jackpotQuery;
    private final JackpotStore jackpotStore;
    private final ContributionSink contributionSink;
    private final ContributionStrategyRegistry contributionRegistry;
    private final BetProcessingGate processingGate;
    private final JackpotPool jackpotPool;

    public JackpotService(JackpotQuery jackpotQuery,
                          JackpotStore jackpotStore,
                          ContributionSink contributionSink,
                          ContributionStrategyRegistry contributionRegistry,
                          BetProcessingGate processingGate,
                          JackpotPool jackpotPool) {
        this.jackpotQuery = jackpotQuery;
        this.jackpotStore = jackpotStore;
        this.contributionSink = contributionSink;
        this.contributionRegistry = contributionRegistry;
        this.processingGate = processingGate;
        this.jackpotPool = jackpotPool;
    }

    /**
     * Called when a bet is consumed from Kafka.
     */
    public void processBet(Bet bet) {
        // Skip if already processed; otherwise acquire short-lived processing lock to avoid races
        if (processingGate.isBetProcessed(bet.getBetId())) {
            return;
        }
        if (!processingGate.tryAcquireProcessing(bet.getBetId(), java.time.Duration.ofSeconds(10))) {
            return;
        }

        Optional<Jackpot> jackpotOpt = jackpotQuery.findJackpotById(bet.getJackpotId());
        if (jackpotOpt.isEmpty()) {
            System.out.println("Jackpot not found for id=" + bet.getJackpotId());
            return;
        }

        Jackpot jackpot = jackpotOpt.get();
        ContributionStrategy strategy = contributionRegistry.get(jackpot.getContributionStrategyType());
        double contribution = strategy.calculateContribution(jackpot, bet.getBetAmount());

        double newPool = jackpotPool.incrementPool(jackpot.getJackpotId(), contribution);
        jackpot.setPoolAmount(newPool);

        // Persist updates
        jackpotStore.saveJackpot(jackpot);

        Contribution record = Contribution.builder()
                .betId(bet.getBetId())
                .userId(bet.getUserId())
                .jackpotId(jackpot.getJackpotId())
                .stakeAmount(bet.getBetAmount())
                .contributionAmount(contribution)
                .currentJackpotAmount(newPool)
                .build();

        contributionSink.appendContribution(record);

        // Mark bet as processed only after successful side effects
        processingGate.markBetProcessed(bet.getBetId());
    }

    // Strategy selection now delegated to ContributionStrategyRegistry
}
