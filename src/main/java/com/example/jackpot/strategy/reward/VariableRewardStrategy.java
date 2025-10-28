package com.example.jackpot.strategy.reward;

import com.example.jackpot.config.RewardStrategyProperties;
import com.example.jackpot.model.Jackpot;
import com.example.jackpot.model.enums.RewardStrategyType;
import com.example.jackpot.strategy.annotations.RewardType;
import com.example.jackpot.strategy.random.RandomGenerator;
import org.springframework.stereotype.Component;

/**
 * Implements a variable reward scheme where the chance to win grows with the pool size until it
 * reaches certainty. Externalized configuration and random generation enable flexible tuning.
 */
@RewardType(RewardStrategyType.VARIABLE)
@Component
public class VariableRewardStrategy implements RewardStrategy {

    private final RewardStrategyProperties.Variable defaults;
    private final RandomGenerator randomGenerator;

    /**
     * Primary constructor keeps the strategy decoupled from the concrete random implementation and
     * ties it to configuration defaults for open extension.
     *
     * @param properties binds variable reward defaults
     * @param randomGenerator supplies random doubles for probability checks
     */
    public VariableRewardStrategy(RewardStrategyProperties properties,
                                  RandomGenerator randomGenerator) {
        this.defaults = properties.getVariable();
        this.randomGenerator = randomGenerator;
    }

    /**
     * Computes the win probability by interpolating between the base chance and certainty as the
     * pool approaches the configured maximum. The resulting probability drives the random outcome.
     *
     * @param jackpot jackpot configuration and current pool
     * @return {@code true} when the random draw results in a win
     */
    @Override
    public boolean isWinner(Jackpot jackpot) {
        // Scale base probability towards certainty as the pool approaches the configured cap.
        double poolForMax = jackpot.getVariableRewardPoolToMaxChance() != null
                ? jackpot.getVariableRewardPoolToMaxChance()
                : defaults.getDefaultPoolToMaxChance();
        poolForMax = poolForMax <= 0 ? defaults.getDefaultPoolToMaxChance() : poolForMax;

        double baseChance = jackpot.getFixedRewardChance() != null
                ? jackpot.getFixedRewardChance()
                : defaults.getDefaultBaseWinProbability();
        baseChance = Math.max(0.0, Math.min(1.0, baseChance));

        double progress = Math.max(0.0, Math.min(1.0, jackpot.getPoolAmount() / poolForMax));
        double probability = baseChance + (1.0 - baseChance) * progress;
        probability = Math.max(0.0, Math.min(1.0, probability));

        return randomGenerator.nextDouble() < probability;
    }

    /**
     * Always pays out the full pool, enabling the jackpot to reset to its initial seed amount.
     *
     * @param jackpot jackpot configuration and pool state
     * @return the entire pool amount
     */
    @Override
    public double calculateReward(Jackpot jackpot) {
        // Variable strategy pays out the entire pool to reset the jackpot to its seed amount.
        return jackpot.getPoolAmount();
    }
}
