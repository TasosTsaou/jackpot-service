package com.example.jackpot.strategy.reward;

import com.example.jackpot.config.RewardStrategyProperties;
import com.example.jackpot.model.Jackpot;
import com.example.jackpot.strategy.random.RandomGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Validates the ramping reward strategy to ensure probability scaling and payouts remain correct.
class VariableRewardStrategyTest {

    private final RewardStrategyProperties properties = new RewardStrategyProperties();
    private final VariableRewardStrategy strategy = new VariableRewardStrategy(
            properties,
            new CyclingRandomGenerator(0.05, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5)
    );

    @Test
    // Ensures the full pool is paid out, preserving the jackpot reset expectation.
    void shouldReturnFullRewardEqualToPool() {
        Jackpot jackpot = Jackpot.builder()
                .jackpotId("J1")
                .poolAmount(5_000.0)
                .variableRewardPoolToMaxChance(1_000_000.0)
                .build();

        double reward = strategy.calculateReward(jackpot);
        assertThat(reward).isEqualTo(5_000.0);
    }

    @Test
    // Verifies the probability scaling produces more winners as the pool approaches the cap.
    void shouldIncreaseWinProbabilityAsPoolIncreases() {
        Jackpot smallPoolJackpot = Jackpot.builder()
                .jackpotId("J-small")
                .poolAmount(10_000.0)
                .variableRewardPoolToMaxChance(1_000_000.0)
                .fixedRewardChance(0.0)
                .build();

        Jackpot largePoolJackpot = Jackpot.builder()
                .jackpotId("J-large")
                .poolAmount(1_000_000.0)
                .variableRewardPoolToMaxChance(1_000_000.0)
                .fixedRewardChance(0.0)
                .build();

        int smallPoolWins = 0;
        int largePoolWins = 0;

        for (int i = 0; i < 1000; i++) {
            if (strategy.isWinner(smallPoolJackpot)) {
                smallPoolWins++;
            }
            if (strategy.isWinner(largePoolJackpot)) {
                largePoolWins++;
            }
        }

        assertThat(largePoolWins).isGreaterThan(smallPoolWins);
    }

    // Test double that replays a known random sequence for reproducible assertions.
    private static final class CyclingRandomGenerator implements RandomGenerator {
        private final double[] values;
        private int index = 0;

        private CyclingRandomGenerator(double... values) {
            this.values = values;
        }

        @Override
        public double nextDouble() {
            // Repeats a known sequence to keep stochastic tests deterministic and reproducible.
            double value = values[index];
            index = (index + 1) % values.length;
            return value;
        }
    }
}
