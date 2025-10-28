package com.example.jackpot.service;

import com.example.jackpot.model.*;
import com.example.jackpot.model.enums.RewardStrategyType;
import com.example.jackpot.port.*;
import com.example.jackpot.strategy.registry.RewardStrategyRegistry;
import com.example.jackpot.strategy.reward.RewardStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RewardServiceTest {

    private BetQuery betQuery;
    private JackpotQuery jackpotQuery;
    private JackpotStore jackpotStore;
    private RewardSink rewardSink;
    private RewardStrategyRegistry registry;
    private RewardEvaluationGate rewardGate;
    private JackpotPool jackpotPool;
    private RewardStrategy strategy;
    private RewardService service;

    @BeforeEach
    void setup() {
        betQuery = mock(BetQuery.class);
        jackpotQuery = mock(JackpotQuery.class);
        jackpotStore = mock(JackpotStore.class);
        rewardSink = mock(RewardSink.class);
        registry = mock(RewardStrategyRegistry.class);
        rewardGate = mock(RewardEvaluationGate.class);
        jackpotPool = mock(JackpotPool.class);
        strategy = mock(RewardStrategy.class);
        service = new RewardService(betQuery, jackpotQuery, jackpotStore, rewardSink, registry, rewardGate, jackpotPool);
    }

    @Test
    void evaluateReward_returnsStored_whenPresent() {
        Reward stored = Reward.builder().betId("b1").jackpotId("j1").userId("u1").jackpotRewardAmount(10.0).createdAt(Instant.now()).build();
        when(rewardGate.findRewardByBetId("b1")).thenReturn(Optional.of(stored));

        Reward out = service.evaluateReward("b1");
        assertThat(out).isSameAs(stored);
        verifyNoInteractions(betQuery, jackpotQuery, registry);
    }

    @Test
    void evaluateReward_throws_whenLockNotAcquiredAndNoStored() {
        when(rewardGate.findRewardByBetId("b1")).thenReturn(Optional.empty());
        when(betQuery.findBet("b1")).thenReturn(Optional.of(new Bet("b1", "u1", "j1", 10.0)));
        when(jackpotQuery.findJackpotById("j1")).thenReturn(Optional.of(Jackpot.builder().jackpotId("j1").poolAmount(100.0).initialAmount(100.0).rewardStrategyType(RewardStrategyType.FIXED).build()));
        when(rewardGate.tryAcquireRewardLock(eq("b1"), any())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.evaluateReward("b1"));
    }

    @Test
    void evaluateReward_winner_flow_persists_and_resets() {
        Bet bet = new Bet("b1", "u1", "j1", 10.0);
        Jackpot jp = Jackpot.builder().jackpotId("j1").poolAmount(100.0).initialAmount(100.0).rewardStrategyType(RewardStrategyType.FIXED).build();
        when(rewardGate.findRewardByBetId("b1")).thenReturn(Optional.empty());
        when(betQuery.findBet("b1")).thenReturn(Optional.of(bet));
        when(jackpotQuery.findJackpotById("j1")).thenReturn(Optional.of(jp));
        when(registry.get(RewardStrategyType.FIXED)).thenReturn(strategy);
        when(rewardGate.tryAcquireRewardLock(eq("b1"), any())).thenReturn(true);
        when(strategy.isWinner(jp)).thenReturn(true);
        when(strategy.calculateReward(jp)).thenReturn(80.0);

        Reward out = service.evaluateReward("b1");

        assertThat(out.getJackpotRewardAmount()).isEqualTo(80.0);
        verify(rewardSink).appendReward(any(Reward.class));
        verify(rewardGate).saveRewardByBetId(any(Reward.class));
        verify(jackpotPool).incrementPool("j1", 0.0); // delta initial(100) - current(100) before reset
        verify(jackpotStore).saveJackpot(argThat(j -> j.getPoolAmount() == 100.0));
    }

    @Test
    void evaluateReward_nonWinner_persistsZeroButDoesNotReset() {
        Bet bet = new Bet("b1", "u1", "j1", 10.0);
        Jackpot jp = Jackpot.builder().jackpotId("j1").poolAmount(100.0).initialAmount(100.0).rewardStrategyType(RewardStrategyType.FIXED).build();
        when(rewardGate.findRewardByBetId("b1")).thenReturn(Optional.empty());
        when(betQuery.findBet("b1")).thenReturn(Optional.of(bet));
        when(jackpotQuery.findJackpotById("j1")).thenReturn(Optional.of(jp));
        when(registry.get(RewardStrategyType.FIXED)).thenReturn(strategy);
        when(rewardGate.tryAcquireRewardLock(eq("b1"), any())).thenReturn(true);
        when(strategy.isWinner(jp)).thenReturn(false);

        Reward out = service.evaluateReward("b1");

        assertThat(out.getJackpotRewardAmount()).isEqualTo(0.0);
        verify(rewardGate).saveRewardByBetId(any(Reward.class));
        verify(rewardSink, never()).appendReward(any());
        verifyNoInteractions(jackpotPool);
        verify(jackpotStore, never()).saveJackpot(any());
    }
}

