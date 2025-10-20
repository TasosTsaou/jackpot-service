package com.example.jackpot.kafka;

import com.example.jackpot.model.Bet;
import com.example.jackpot.service.JackpotService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer that processes bet events and delegates to the jackpot service.
 */
@Service
public class BetConsumer {
    private final JackpotService jackpotService;

    public BetConsumer(JackpotService jackpotService) {
        this.jackpotService = jackpotService;
    }

    @KafkaListener(topics = "jackpot-bets", groupId = "jackpot-consumers")
    public void consumeBet(Bet bet) {
        jackpotService.processBet(bet);
    }
}
