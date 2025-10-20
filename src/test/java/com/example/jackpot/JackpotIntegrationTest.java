package com.example.jackpot;

import com.example.jackpot.model.*;
import com.example.jackpot.model.enums.*;
import com.example.jackpot.port.*;
import com.example.jackpot.service.*;
import com.example.jackpot.strategy.contribution.FixedContributionStrategy;
import com.example.jackpot.strategy.contribution.VariableContributionStrategy;
import com.example.jackpot.strategy.random.RandomGenerator;
import com.example.jackpot.strategy.reward.FixedRewardStrategy;
import com.example.jackpot.strategy.reward.VariableRewardStrategy;
import com.example.jackpot.config.RewardStrategyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test (in-memory): verifies bet -> contribution update -> reward evaluation
 * using in-memory port adapters to avoid external Kafka/Redis dependencies.
 *
 * Test flow overview:
 * 1) Seed a jackpot (J1) with fixed strategies and known parameters.
 * 2) Submit a bet via {@link BetService#handleBet(Bet)} to persist it.
 * 3) Simulate the Kafka consumer by invoking {@link JackpotService#processBet(Bet)} to apply the
 *    contribution and increase the pool.
 * 4) Call {@link RewardService#evaluateReward(String)} to determine the outcome and payout.
 * 5) Assert that on a win the reward amount is positive and the pool is reset to the initial value.
 */
@SpringBootTest(classes = {
        JackpotIntegrationTest.InMemoryPortsConfig.class,
        BetService.class,
        JackpotService.class,
        RewardService.class,
        FixedContributionStrategy.class,
        VariableContributionStrategy.class,
        FixedRewardStrategy.class,
        VariableRewardStrategy.class,
        RewardStrategyProperties.class
})
class JackpotIntegrationTest {

    @Configuration
    static class InMemoryPortsConfig {
        @Bean
        @Primary
        BetStore betStore() { return new InMemoryBetStore(); }

        @Bean
        @Primary
        BetQuery betQuery(BetStore store) { return ((InMemoryBetStore) store)::find; }

        @Bean
        @Primary
        JackpotStore jackpotStore() { return new InMemoryJackpotStore(); }

        @Bean
        @Primary
        JackpotQuery jackpotQuery(JackpotStore store) { return ((InMemoryJackpotStore) store)::findById; }

        @Bean
        @Primary
        ContributionSink contributionSink() { return new InMemoryContributionSink(); }

        @Bean
        @Primary
        RewardSink rewardSink() { return new InMemoryRewardSink(); }

        @Bean
        @Primary
        BetPublisher betPublisher() { return bet -> { /* no-op in-memory */ }; }

        // Deterministic RNG to keep behavior stable if defaults are used elsewhere
        @Bean
        @Primary
        RandomGenerator randomGenerator() { return () -> 0.0; }
    }

    // --- In-memory implementations ---
    static class InMemoryBetStore implements BetStore {
        private final Map<String, Bet> bets = new HashMap<>();
        @Override public void save(Bet bet) { bets.put(bet.getBetId(), bet); }
        Optional<Bet> find(String id) { return Optional.ofNullable(bets.get(id)); }
    }

    static class InMemoryJackpotStore implements JackpotStore {
        private final Map<String, Jackpot> jackpots = new HashMap<>();
        @Override public void saveJackpot(Jackpot jackpot) { jackpots.put(jackpot.getJackpotId(), jackpot); }
        Optional<Jackpot> findById(String id) { return Optional.ofNullable(jackpots.get(id)); }
    }

    static class InMemoryContributionSink implements ContributionSink {
        final List<Contribution> records = new ArrayList<>();
        @Override public void appendContribution(Contribution c) { records.add(c); }
    }

    static class InMemoryRewardSink implements RewardSink {
        final List<Reward> records = new ArrayList<>();
        @Override public void appendReward(Reward r) { records.add(r); }
    }

    @Autowired private BetService betService;
    @Autowired private JackpotService jackpotService;
    @Autowired private RewardService rewardService;
    @Autowired private JackpotStore jackpotStore;

    /**
     * Seeds a fixed-strategy jackpot with deterministic winning chance (100%) to keep assertions
     * stable and verify the pool reset behavior after a win.
     */
    @BeforeEach
    void setupJackpot() {
        Jackpot jackpot = Jackpot.builder()
                .jackpotId("J1")
                .initialAmount(1000.0)
                .poolAmount(1000.0)
                .contributionStrategyType(ContributionStrategyType.FIXED)
                .rewardStrategyType(RewardStrategyType.FIXED)
                .fixedContributionPercent(0.05)
                .fixedRewardChance(1.0) // force a win for deterministic assertions
                .fixedRewardPayoutPercent(0.8)
                .build();
        jackpotStore.saveJackpot(jackpot);
    }

    /**
     * End-to-end happy-path covering: store bet -> process contribution -> evaluate reward -> reset pool.
     */
    @Test
    void endToEndBetFlowShouldUpdatePoolAndEvaluateReward() {
        Bet bet = new Bet("B100", "U1", "J1", 200.0);

        // Step 1: Publish bet (stored via BetStore)
        betService.handleBet(bet);

        // Step 2: Simulate Kafka consumer updating jackpot
        jackpotService.processBet(bet);

        // Step 3: Evaluate reward (forced win)
        Reward reward = rewardService.evaluateReward("B100");

        assertThat(reward).isNotNull();
        assertThat(reward.getJackpotId()).isEqualTo("J1");
        assertThat(reward.getCreatedAt()).isNotNull();
        assertThat(reward.getJackpotRewardAmount()).isGreaterThan(0.0);

        // Step 4: Pool resets to initial value after win
        Jackpot jackpot = ((InMemoryJackpotStore) jackpotStore).findById("J1").orElseThrow();
        assertThat(jackpot.getPoolAmount()).isEqualTo(1000.0);
    }
}
