package com.example.jackpot.service;

import com.example.jackpot.model.*;
import com.example.jackpot.model.enums.ContributionStrategyType;
import com.example.jackpot.port.ContributionSink;
import com.example.jackpot.port.JackpotQuery;
import com.example.jackpot.port.JackpotStore;
import com.example.jackpot.strategy.contribution.ContributionStrategy;
import com.example.jackpot.strategy.contribution.FixedContributionStrategy;
import com.example.jackpot.strategy.contribution.VariableContributionStrategy;
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
    private final FixedContributionStrategy fixedContribution;
    private final VariableContributionStrategy variableContribution;

    public JackpotService(JackpotQuery jackpotQuery,
                          JackpotStore jackpotStore,
                          ContributionSink contributionSink,
                          FixedContributionStrategy fixedContribution,
                          VariableContributionStrategy variableContribution) {
        this.jackpotQuery = jackpotQuery;
        this.jackpotStore = jackpotStore;
        this.contributionSink = contributionSink;
        this.fixedContribution = fixedContribution;
        this.variableContribution = variableContribution;
    }

    /**
     * Called when a bet is consumed from Kafka.
     */
    public void processBet(Bet bet) {
        Optional<Jackpot> jackpotOpt = jackpotQuery.findJackpotById(bet.getJackpotId());
        if (jackpotOpt.isEmpty()) {
            System.out.println("Jackpot not found for id=" + bet.getJackpotId());
            return;
        }

        Jackpot jackpot = jackpotOpt.get();
        ContributionStrategy strategy = selectStrategy(jackpot.getContributionStrategyType());
        double contribution = strategy.calculateContribution(jackpot, bet.getBetAmount());

        double newPool = jackpot.getPoolAmount() + contribution;
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
    }

    private ContributionStrategy selectStrategy(ContributionStrategyType type) {
        return switch (type) {
            case VARIABLE -> variableContribution;
            case FIXED -> fixedContribution;
        };
    }
}





