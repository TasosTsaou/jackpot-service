package com.example.jackpot.kafka;

import com.example.jackpot.model.Bet;
import com.example.jackpot.service.JackpotService;
import com.example.jackpot.service.RewardService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer that processes bet events and delegates to the jackpot service.
 */
@Service
public class BetConsumer {
    private final JackpotService jackpotService;
    private final RewardService rewardService;

    public BetConsumer(JackpotService jackpotService, RewardService rewardService) {
        this.jackpotService = jackpotService;
        this.rewardService = rewardService;
    }

    /**
     * Consumes bet events and forwards them to domain services.
     *
     * Flow (SOLID, hexagonal):
     * - JackpotService: applies contribution and persists updated state via ports.
     * - RewardService: evaluates reward idempotently, persists by betId, and resets pool on win.
     *
     * Ordering per jackpot is preserved by keying on {@code jackpotId} at the producer side.
     */
    @KafkaListener(topics = "${app.kafka.topic-name:jackpot-bets}", groupId = "${spring.kafka.consumer.group-id:jackpot-consumers}", concurrency = "${app.kafka.concurrency:1}")
    public void consumeBet(Bet bet) {
        jackpotService.processBet(bet);
        // Compute reward as part of the consume path; RewardService is idempotent by betId
        rewardService.evaluateReward(bet.getBetId());
    }
}
