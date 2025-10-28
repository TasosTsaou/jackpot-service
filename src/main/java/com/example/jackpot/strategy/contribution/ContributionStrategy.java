package com.example.jackpot.strategy.contribution;

import com.example.jackpot.model.Jackpot;

/**
 * Strategy interface for calculating how much a bet contributes to a jackpot's pool.
 */
public interface ContributionStrategy {
    /**
     * Computes the amount from the given bet that should be contributed to the jackpot pool
     * according to the configured strategy and the jackpot's current state.
     *
     * Implementations must not mutate the provided jackpot; callers apply the returned value to
     * update persistence. The returned value should be non-negative.
     *
     * @param jackpot   current jackpot configuration and pool amount
     * @param betAmount the stake placed by the player
     * @return the contribution value to add to the jackpot pool
     */
    double calculateContribution(Jackpot jackpot, double betAmount);
}
