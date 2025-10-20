package com.example.jackpot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Holds configurable defaults for reward strategy behavior. These values can be
 * overridden via {@code application.yml}.
 */
@Component
@ConfigurationProperties(prefix = "reward.strategy")
public class RewardStrategyProperties {

    private final Fixed fixed = new Fixed();
    private final Variable variable = new Variable();

    /**
     * @return configuration block for the fixed reward strategy defaults.
     */
    public Fixed getFixed() {
        return fixed;
    }

    /**
     * @return configuration block for the variable reward strategy defaults.
     */
    public Variable getVariable() {
        return variable;
    }

    /**
     * Captures defaults that steer the fixed reward strategy's win rate and payouts.
     */
    public static class Fixed {
        private double defaultWinProbability = 0.1;
        private double defaultPayoutPercent = 0.8;

        /**
         * @return baseline win probability when jackpots do not specify an override.
         */
        public double getDefaultWinProbability() {
            return defaultWinProbability;
        }

        /**
         * Records the default win probability so environments can tune difficulty.
         */
        public void setDefaultWinProbability(double defaultWinProbability) {
            this.defaultWinProbability = defaultWinProbability;
        }

        /**
         * @return default percentage of the pool awarded on a win.
         */
        public double getDefaultPayoutPercent() {
            return defaultPayoutPercent;
        }

        /**
         * Records the payout percentage to control how quickly jackpots deplete.
         */
        public void setDefaultPayoutPercent(double defaultPayoutPercent) {
            this.defaultPayoutPercent = defaultPayoutPercent;
        }
    }

    /**
     * Holds configuration that governs how the variable reward strategy ramps winning odds.
     */
    public static class Variable {
        private double defaultPoolToMaxChance = 1_000_000.0;
        private double defaultBaseWinProbability = 0.0;

        /**
         * @return pool size where the variable strategy hits 100% win probability.
         */
        public double getDefaultPoolToMaxChance() {
            return defaultPoolToMaxChance;
        }

        /**
         * Sets the cap that determines how quickly win probability ramps up.
         */
        public void setDefaultPoolToMaxChance(double defaultPoolToMaxChance) {
            this.defaultPoolToMaxChance = defaultPoolToMaxChance;
        }

        /**
         * @return fallback base win chance before pool-based scaling is applied.
         */
        public double getDefaultBaseWinProbability() {
            return defaultBaseWinProbability;
        }

        /**
         * Persists the base chance to control how often wins occur at small pool sizes.
         */
        public void setDefaultBaseWinProbability(double defaultBaseWinProbability) {
            this.defaultBaseWinProbability = defaultBaseWinProbability;
        }
    }
}
