package com.example.jackpot.strategy.reward;

import com.example.jackpot.config.RewardStrategyProperties;
import com.example.jackpot.model.Jackpot;
import com.example.jackpot.strategy.random.RandomGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Covers FixedRewardStrategy behaviour to guarantee configurable defaults and consistent payouts.
class FixedRewardStrategyTest {

    private final RewardStrategyProperties properties = new RewardStrategyProperties();
    private final FixedRewardStrategy strategy = new FixedRewardStrategy(
            properties,
            new CyclingRandomGenerator(0.05, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5)
    );

    @Test
    // Verifies deterministic reward calculation when a fixed payout percent is supplied.
    void shouldCalculateConfiguredReward() {
        Jackpot jackpot = Jackpot.builder()
                .jackpotId("J1")
                .poolAmount(1_000.0)
                .fixedRewardPayoutPercent(0.8)
                .build();

        double reward = strategy.calculateReward(jackpot);
        assertThat(reward).isEqualTo(800.0);
    }

    @Test
    // Validates that missing per-jackpot chance falls back to the configured default.
    void shouldUseConfiguredDefaultWhenChanceMissing() {
        Jackpot jackpot = Jackpot.builder()
                .jackpotId("J-default")
                .poolAmount(1_000.0)
                .build();

        boolean isWinner = strategy.isWinner(jackpot);

        assertThat(isWinner).isTrue();
    }

    @Test
    // Ensures default payout percent is applied when jackpots omit explicit configuration.
    void shouldUseDefaultPayoutWhenNotProvided() {
        properties.getFixed().setDefaultPayoutPercent(0.75);

        Jackpot jackpot = Jackpot.builder()
                .jackpotId("J1")
                .poolAmount(2_000.0)
                .build();

        double reward = strategy.calculateReward(jackpot);

        assertThat(reward).isEqualTo(1_500.0);
    }

    @Test
    // Confirms the win rate converges near the configured chance when randomness is injected.
    void shouldProduceWinnerRoughlyTenPercentOverManyRuns() {
        Jackpot jackpot = Jackpot.builder()
                .jackpotId("J1")
                .poolAmount(1_000.0)
                .fixedRewardChance(0.1)
                .build();

        int wins = 0;
        for (int i = 0; i < 1000; i++) {
            if (strategy.isWinner(jackpot)) {
                wins++;
            }
        }
        assertThat(wins).isBetween(70, 130);
    }

    // Test double providing deterministic random values by looping over a pre-defined sequence.
    private static final class CyclingRandomGenerator implements RandomGenerator {
        private final double[] values;
        private int index = 0;

        private CyclingRandomGenerator(double... values) {
            this.values = values;
        }

        @Override
        public double nextDouble() {
            // Cycles through the provided sequence so probability assertions remain deterministic.
            double value = values[index];
            index = (index + 1) % values.length;
            return value;
        }
    }
}
