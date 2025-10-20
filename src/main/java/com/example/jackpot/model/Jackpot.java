package com.example.jackpot.model;

import com.example.jackpot.model.enums.ContributionStrategyType;
import com.example.jackpot.model.enums.RewardStrategyType;
import lombok.*;

import java.io.Serializable;

/**
 * Core jackpot state and configuration used by contribution and reward strategies.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Jackpot implements Serializable {
    private String jackpotId;

    // Current and initial pool
    private double poolAmount;
    private double initialAmount;

    // Which strategies this jackpot uses
    private ContributionStrategyType contributionStrategyType;
    private RewardStrategyType rewardStrategyType;

    // Config knobs (tune as needed per strategy implementation)
    // Fixed contribution % (e.g., 0.05 means 5%)
    private Double fixedContributionPercent;

    // Variable contribution config (example: decreases with pool)
    private Double variableContributionStartPercent; // e.g., 0.10 (10%)
    private Double variableContributionMinPercent;   // e.g., 0.01 (1%)
    private Double variableContributionDecayPool;    // e.g., 1_000_000.0

    // Fixed reward config
    private Double fixedRewardChance;                // e.g., 0.1 means 10%
    private Double fixedRewardPayoutPercent;         // e.g., 0.8 pays 80% of pool

    // Variable reward config (e.g., scales with pool; 100% chance after limit)
    private Double variableRewardPoolToMaxChance;    // pool at which chance hits 100%
}
