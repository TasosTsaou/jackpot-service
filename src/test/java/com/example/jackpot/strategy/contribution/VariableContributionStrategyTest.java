package com.example.jackpot.strategy.contribution;

import com.example.jackpot.model.Jackpot;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VariableContributionStrategyTest {

    private final VariableContributionStrategy strategy = new VariableContributionStrategy();

    @Test
    void shouldReturnHighContributionWhenPoolIsLow() {
        double bet = 100.0;
        Jackpot jackpot = Jackpot.builder()
                .jackpotId("J-low")
                .poolAmount(1_000.0)
                .variableContributionStartPercent(0.10)
                .variableContributionMinPercent(0.01)
                .variableContributionDecayPool(1_000_000.0)
                .build();

        double result = strategy.calculateContribution(jackpot, bet);
        assertThat(result).isGreaterThan(5.0);
    }

    @Test
    void shouldDecreaseContributionWhenPoolIsHigh() {
        double bet = 100.0;

        Jackpot lowPoolJackpot = Jackpot.builder()
                .jackpotId("J-low")
                .poolAmount(10_000.0)
                .variableContributionStartPercent(0.10)
                .variableContributionMinPercent(0.01)
                .variableContributionDecayPool(1_000_000.0)
                .build();

        Jackpot highPoolJackpot = Jackpot.builder()
                .jackpotId("J-high")
                .poolAmount(900_000.0)
                .variableContributionStartPercent(0.10)
                .variableContributionMinPercent(0.01)
                .variableContributionDecayPool(1_000_000.0)
                .build();

        double lowPoolContribution = strategy.calculateContribution(lowPoolJackpot, bet);
        double highPoolContribution = strategy.calculateContribution(highPoolJackpot, bet);

        assertThat(highPoolContribution).isLessThan(lowPoolContribution);
    }
}
