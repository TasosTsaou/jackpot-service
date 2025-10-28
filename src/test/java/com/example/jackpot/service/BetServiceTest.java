package com.example.jackpot.service;

import com.example.jackpot.model.*;
import com.example.jackpot.port.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BetServiceTest {

    private BetStore betStore;
    private BetPublisher betPublisher;
    private BetProcessingGate processingGate;

    private BetService service;

    @BeforeEach
    void setup() {
        betStore = mock(BetStore.class);
        betPublisher = mock(BetPublisher.class);
        processingGate = mock(BetProcessingGate.class);
        service = new BetService(betStore, betPublisher, processingGate);
    }

    @Test
    void handleBet_skipsWhenAlreadyProcessed() {
        when(processingGate.isBetProcessed("b1")).thenReturn(true);

        service.handleBet(new Bet("b1", "u1", "j1", 10.0));

        verifyNoInteractions(betStore, betPublisher);
    }

    @Test
    void handleBet_persistsAndPublishesWhenNotProcessed() {
        when(processingGate.isBetProcessed("b1")).thenReturn(false);

        Bet bet = new Bet("b1", "u1", "j1", 10.0);
        service.handleBet(bet);

        verify(betStore).save(bet);
        verify(betPublisher).publish(bet);
    }
}
