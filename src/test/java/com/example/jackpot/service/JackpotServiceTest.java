package com.example.jackpot.service;

import com.example.jackpot.model.Bet;
import com.example.jackpot.model.Jackpot;
import com.example.jackpot.model.enums.ContributionStrategyType;
import com.example.jackpot.port.*;
import com.example.jackpot.strategy.contribution.ContributionStrategy;
import com.example.jackpot.strategy.registry.ContributionStrategyRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JackpotServiceTest {

    private JackpotQuery jackpotQuery;
    private JackpotStore jackpotStore;
    private ContributionSink contributionSink;
    private ContributionStrategyRegistry registry;
    private BetProcessingGate processingGate;
    private JackpotPool jackpotPool;
    private ContributionStrategy strategy;
    private JackpotService service;

    @BeforeEach
    void setup() {
        jackpotQuery = mock(JackpotQuery.class);
        jackpotStore = mock(JackpotStore.class);
        contributionSink = mock(ContributionSink.class);
        registry = mock(ContributionStrategyRegistry.class);
        processingGate = mock(BetProcessingGate.class);
        jackpotPool = mock(JackpotPool.class);
        strategy = mock(ContributionStrategy.class);
        service = new JackpotService(jackpotQuery, jackpotStore, contributionSink, registry, processingGate, jackpotPool);
    }

    @Test
    void processBet_skips_whenAlreadyProcessed() {
        Bet bet = new Bet("b1", "u1", "j1", 50.0);
        when(processingGate.isBetProcessed("b1")).thenReturn(true);

        service.processBet(bet);

        verifyNoInteractions(jackpotQuery, registry, jackpotPool, contributionSink, jackpotStore);
    }

    @Test
    void processBet_skips_whenLockNotAcquired() {
        Bet bet = new Bet("b1", "u1", "j1", 50.0);
        when(processingGate.isBetProcessed("b1")).thenReturn(false);
        when(processingGate.tryAcquireProcessing(eq("b1"), any())).thenReturn(false);

        service.processBet(bet);

        verifyNoInteractions(jackpotQuery, registry, jackpotPool, contributionSink, jackpotStore);
    }

    @Test
    void processBet_happyPath_updatesPool_appendsContribution_marksProcessed() {
        Bet bet = new Bet("b1", "u1", "j1", 50.0);
        Jackpot jackpot = Jackpot.builder()
                .jackpotId("j1")
                .poolAmount(100.0)
                .initialAmount(100.0)
                .contributionStrategyType(ContributionStrategyType.FIXED)
                .build();

        when(processingGate.isBetProcessed("b1")).thenReturn(false);
        when(processingGate.tryAcquireProcessing(eq("b1"), any())).thenReturn(true);
        when(jackpotQuery.findJackpotById("j1")).thenReturn(Optional.of(jackpot));
        when(registry.get(ContributionStrategyType.FIXED)).thenReturn(strategy);
        when(strategy.calculateContribution(jackpot, 50.0)).thenReturn(5.0);
        when(jackpotPool.incrementPool("j1", 5.0)).thenReturn(105.0);

        service.processBet(bet);

        verify(jackpotPool).incrementPool("j1", 5.0);
        verify(jackpotStore).saveJackpot(argThat(j -> j.getPoolAmount() == 105.0));
        verify(contributionSink).appendContribution(argThat(c -> c.getContributionAmount() == 5.0 && c.getCurrentJackpotAmount() == 105.0));
        verify(processingGate).markBetProcessed("b1");
    }
}

