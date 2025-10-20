package com.example.jackpot.strategy.contribution;

import com.example.jackpot.model.Jackpot;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FixedContributionStrategyTest {

    private final FixedContributionStrategy strategy = new FixedContributionStrategy();

    @Test
    void shouldCalculateConfiguredContributionPercentage() {
        double bet = 200.0;
        Jackpot jackpot = Jackpot.builder()
                .jackpotId("J1")
                .poolAmount(1_000.0)
                .fixedContributionPercent(0.05)
                .build();

        double result = strategy.calculateContribution(jackpot, bet);
        assertThat(result).isEqualTo(10.0);
    }

    @Test
    void shouldReturnZeroIfBetIsZero() {
        Jackpot jackpot = Jackpot.builder()
                .jackpotId("J1")
                .fixedContributionPercent(0.05)
                .build();

        assertThat(strategy.calculateContribution(jackpot, 0.0)).isEqualTo(0.0);
    }
}
