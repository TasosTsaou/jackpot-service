package com.example.jackpot.strategy.reward;

import com.example.jackpot.model.Jackpot;

/**
 * Strategy interface for determining wins and computing payout amounts.
 */
public interface RewardStrategy {
    /**
     * Determines whether a given jackpot configuration and current pool state produce a winning
     * outcome for the evaluated bet. Implementations should be deterministic for a fixed random
     * source and respect probability bounds [0.0, 1.0].
     *
     * @param jackpot the jackpot configuration and current pool state
     * @return true if the bet results in a win; false otherwise
     */
    boolean isWinner(Jackpot jackpot);

    /**
     * Calculates the payout amount for a winning bet based on the supplied jackpot's current
     * state and the strategy's rules. Implementations should not mutate the jackpot; callers are
     * responsible for any state changes (e.g., pool reset) after payout is computed.
     *
     * @param jackpot the jackpot configuration and current pool state
     * @return a non-negative monetary value to pay to the winner
     */
    double calculateReward(Jackpot jackpot);
}
