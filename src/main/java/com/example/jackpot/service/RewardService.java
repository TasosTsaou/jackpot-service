package com.example.jackpot.service;

import com.example.jackpot.model.*;
import com.example.jackpot.model.enums.RewardStrategyType;
import com.example.jackpot.port.BetQuery;
import com.example.jackpot.port.JackpotQuery;
import com.example.jackpot.port.JackpotStore;
import com.example.jackpot.port.RewardSink;
import com.example.jackpot.strategy.registry.RewardStrategyRegistry;
import com.example.jackpot.strategy.reward.RewardStrategy;
import com.example.jackpot.port.RewardEvaluationGate;
import com.example.jackpot.port.JackpotPool;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Application service that evaluates rewards for bets by orchestrating reads,
 * strategy decisions, and persistence through ports.
 */
@Service
public class RewardService {

    private final BetQuery betQuery;
    private final JackpotQuery jackpotQuery;
    private final JackpotStore jackpotStore;
    private final RewardSink rewardSink;
    private final RewardStrategyRegistry rewardRegistry;
    private final RewardEvaluationGate rewardGate;
    private final JackpotPool jackpotPool;

    /**
     * Connects domain logic through ports so implementation details can vary independently.
     */
    public RewardService(BetQuery betQuery,
                         JackpotQuery jackpotQuery,
                         JackpotStore jackpotStore,
                         RewardSink rewardSink,
                         RewardStrategyRegistry rewardRegistry,
                         RewardEvaluationGate rewardGate,
                         JackpotPool jackpotPool) {
        this.betQuery = betQuery;
        this.jackpotQuery = jackpotQuery;
        this.jackpotStore = jackpotStore;
        this.rewardSink = rewardSink;
        this.rewardRegistry = rewardRegistry;
        this.rewardGate = rewardGate;
        this.jackpotPool = jackpotPool;
    }

    /**
     * Evaluate if a given bet wins a reward.
     *
     * @param betId identifier of the bet to evaluate
     * @return reward record describing either the payout or the zero-result when not a winner
     */
    public Reward evaluateReward(String betId) {
        // Fast path: if a reward has already been computed and stored by bet id, return it.
        Optional<Reward> existing = rewardGate.findRewardByBetId(betId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Look up the bet and associated jackpot; missing data is treated as a client error.
        Optional<Bet> betOpt = betQuery.findBet(betId);
        if (betOpt.isEmpty()) {
            throw new IllegalArgumentException("Bet not found: " + betId);
        }

        Bet bet = betOpt.get();
        Jackpot jackpot = jackpotQuery.findJackpotById(bet.getJackpotId())
                .orElseThrow(() -> new IllegalArgumentException("Jackpot not found: " + bet.getJackpotId()));

        RewardStrategy strategy = rewardRegistry.get(jackpot.getRewardStrategyType());

        // Acquire a short-lived lock to ensure single computation
        boolean locked = rewardGate.tryAcquireRewardLock(betId, java.time.Duration.ofSeconds(10));
        if (!locked) {
            Optional<Reward> maybe = rewardGate.findRewardByBetId(betId);
            return maybe.orElseThrow(() -> new IllegalArgumentException("Reward evaluation in progress for bet: " + betId));
        }

        if (strategy.isWinner(jackpot)) {
            // Persist the win, pay out via strategy-specific calculation, and reset the pool.
            double rewardAmount = strategy.calculateReward(jackpot);

            Reward reward = Reward.builder()
                    .betId(bet.getBetId())
                    .userId(bet.getUserId())
                    .jackpotId(jackpot.getJackpotId())
                    .jackpotRewardAmount(rewardAmount)
                    .createdAt(Instant.now())
                    .build();

            rewardSink.appendReward(reward);

            // Persist reward by bet for idempotency
            rewardGate.saveRewardByBetId(reward);

            // Reset the pool atomically
            double delta = jackpot.getInitialAmount() - jackpot.getPoolAmount();
            jackpotPool.incrementPool(jackpot.getJackpotId(), delta);
            jackpot.setPoolAmount(jackpot.getInitialAmount());
            jackpotStore.saveJackpot(jackpot);

            return reward;
        } else {
            // Non-winning bets are recorded with a zero reward to provide an audit trail.
            Reward noWin = Reward.builder()
                    .betId(bet.getBetId())
                    .userId(bet.getUserId())
                    .jackpotId(bet.getJackpotId())
                    .jackpotRewardAmount(0.0)
                    .createdAt(Instant.now())
                    .build();
            // Persist reward by bet for idempotency
            rewardGate.saveRewardByBetId(noWin);
            return noWin;
        }
    }

    /**
     * Selects the configured strategy, keeping the decision isolated so new variants slot in
     * without affecting callers (open/closed principle).
     */
    // Strategy selection now delegated to RewardStrategyRegistry
}
