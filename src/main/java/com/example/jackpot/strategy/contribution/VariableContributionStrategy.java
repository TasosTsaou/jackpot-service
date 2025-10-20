package com.example.jackpot.strategy.contribution;

import com.example.jackpot.model.Jackpot;
import org.springframework.stereotype.Component;

/**
 * Calculates a contribution that decreases from a starting percentage towards a
 * minimum as the jackpot pool grows.
 */
@Component
public class VariableContributionStrategy implements ContributionStrategy {
    private static final double DEFAULT_START_PERCENT = 0.10;
    private static final double DEFAULT_MIN_PERCENT = 0.01;
    private static final double DEFAULT_DECAY_POOL = 1_000_000.0;

    @Override
    public double calculateContribution(Jackpot jackpot, double betAmount) {
        double startPercent = jackpot.getVariableContributionStartPercent() != null
                ? jackpot.getVariableContributionStartPercent()
                : DEFAULT_START_PERCENT;
        double minPercent = jackpot.getVariableContributionMinPercent() != null
                ? jackpot.getVariableContributionMinPercent()
                : DEFAULT_MIN_PERCENT;
        double decayPool = jackpot.getVariableContributionDecayPool() != null
                ? jackpot.getVariableContributionDecayPool()
                : DEFAULT_DECAY_POOL;

        startPercent = Math.max(0.0, startPercent);
        minPercent = Math.min(startPercent, Math.max(0.0, minPercent));
        decayPool = decayPool <= 0 ? DEFAULT_DECAY_POOL : decayPool;

        double currentPool = Math.max(0.0, jackpot.getPoolAmount());
        double progress = Math.min(1.0, currentPool / decayPool);
        double percent = startPercent - (startPercent - minPercent) * progress;
        percent = Math.max(minPercent, Math.min(startPercent, percent));

        return betAmount * percent;
    }
}
