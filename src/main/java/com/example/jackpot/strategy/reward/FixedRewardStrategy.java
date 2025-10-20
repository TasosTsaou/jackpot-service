package com.example.jackpot.strategy.reward;

import com.example.jackpot.config.RewardStrategyProperties;
import com.example.jackpot.model.Jackpot;
import com.example.jackpot.strategy.random.RandomGenerator;
import org.springframework.stereotype.Component;

/**
 * Implements the fixed reward calculation: every bet has the same win probability and payouts
 * represent a configurable percentage of the current jackpot pool. Configuration and randomness
 * are injected to respect dependency inversion and keep the strategy open to extension.
 */
@Component
public class FixedRewardStrategy implements RewardStrategy {

    private final RewardStrategyProperties.Fixed defaults;
    private final RandomGenerator randomGenerator;

    /**
     * Primary constructor used by Spring. Injects configuration defaults and a random generator to
     * keep the strategy open for extension yet closed for direct modifications (OCP).
     *
     * @param properties binds configurable defaults
     * @param randomGenerator supplies random doubles for win checks
     */
    public FixedRewardStrategy(RewardStrategyProperties properties,
                               RandomGenerator randomGenerator) {
        this.defaults = properties.getFixed();
        this.randomGenerator = randomGenerator;
    }

    /**
     * Determines whether the supplied jackpot produces a winning outcome using the configured
     * probability. Jackpot-specific overrides take precedence over defaults.
     *
     * @param jackpot jackpot configuration and current pool
     * @return {@code true} when the random draw results in a win
     */
    @Override
    public boolean isWinner(Jackpot jackpot) {
        // Determine probability from per-jackpot override falling back to configurable defaults.
        double probability = jackpot.getFixedRewardChance() != null
                ? jackpot.getFixedRewardChance()
                : defaults.getDefaultWinProbability();
        probability = Math.max(0.0, Math.min(1.0, probability));
        return randomGenerator.nextDouble() < probability;
    }

    /**
     * Calculates the payout amount for a winning bet as a fixed percentage of the current pool.
     * Negative percentages are coerced to zero to avoid draining the pool.
     *
     * @param jackpot jackpot configuration and pool state
     * @return reward amount to grant the player
     */
    @Override
    public double calculateReward(Jackpot jackpot) {
        // Pay out a fixed percentage of the current pool, ensuring non-negative payouts.
        double payoutPercent = jackpot.getFixedRewardPayoutPercent() != null
                ? jackpot.getFixedRewardPayoutPercent()
                : defaults.getDefaultPayoutPercent();
        payoutPercent = Math.max(0.0, payoutPercent);
        return jackpot.getPoolAmount() * payoutPercent;
    }
}
