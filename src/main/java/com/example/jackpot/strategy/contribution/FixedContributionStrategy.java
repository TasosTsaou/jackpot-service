package com.example.jackpot.strategy.contribution;

import com.example.jackpot.model.Jackpot;
import org.springframework.stereotype.Component;

/**
 * Calculates a fixed percentage of the bet amount as a contribution to the pool.
 */
@Component
public class FixedContributionStrategy implements ContributionStrategy {
    private static final double DEFAULT_PERCENT = 0.05; // fallback 5%

    @Override
    public double calculateContribution(Jackpot jackpot, double betAmount) {
        double percent = jackpot.getFixedContributionPercent() != null
                ? jackpot.getFixedContributionPercent()
                : DEFAULT_PERCENT;
        percent = Math.max(0.0, percent);
        return betAmount * percent;
    }
}
