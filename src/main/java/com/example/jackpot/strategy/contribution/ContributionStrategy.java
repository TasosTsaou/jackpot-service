package com.example.jackpot.strategy.contribution;

import com.example.jackpot.model.Jackpot;

/**
 * Strategy interface for calculating how much a bet contributes to a jackpot's pool.
 */
public interface ContributionStrategy {
    double calculateContribution(Jackpot jackpot, double betAmount);
}
