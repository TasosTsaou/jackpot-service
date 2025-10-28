package com.example.jackpot.kafka;

import com.example.jackpot.model.Bet;
import com.example.jackpot.service.JackpotService;
import com.example.jackpot.service.RewardService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class BetConsumerTest {

    @Test
    void consumeBet_invokesContributionThenReward() {
        JackpotService jackpotService = mock(JackpotService.class);
        RewardService rewardService = mock(RewardService.class);
        BetConsumer consumer = new BetConsumer(jackpotService, rewardService);

        Bet bet = new Bet("b1", "u1", "j1", 10.0);

        consumer.consumeBet(bet);

        verify(jackpotService).processBet(bet);
        verify(rewardService).evaluateReward("b1");
        verifyNoMoreInteractions(jackpotService, rewardService);
    }
}

