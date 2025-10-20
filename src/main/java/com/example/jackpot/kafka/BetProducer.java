package com.example.jackpot.kafka;

import com.example.jackpot.model.Bet;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka producer for publishing bet events to the configured topic.
 */
@Service
public class BetProducer {
    private final KafkaTemplate<String, Bet> kafkaTemplate;

    public BetProducer(KafkaTemplate<String, Bet> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendBet(Bet bet) {
        kafkaTemplate.send("jackpot-bets", bet.getBetId(), bet);
    }
}
