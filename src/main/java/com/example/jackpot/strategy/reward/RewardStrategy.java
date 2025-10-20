package com.example.jackpot.strategy.reward;

import com.example.jackpot.model.Jackpot;

/**
 * Strategy interface for determining wins and computing payout amounts.
 */
public interface RewardStrategy {
    boolean isWinner(Jackpot jackpot);
    double calculateReward(Jackpot jackpot);
}
