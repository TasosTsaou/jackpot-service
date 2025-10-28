package com.example.jackpot.kafka;

import com.example.jackpot.model.Bet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka producer for publishing bet events to the configured topic.
 */
@Service
public class BetProducer {
    private final KafkaTemplate<String, Bet> kafkaTemplate;
    private final String topicName;

    public BetProducer(KafkaTemplate<String, Bet> kafkaTemplate,
                       @Value("${app.kafka.topic-name:jackpot-bets}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void sendBet(Bet bet) {
        // Key by jackpotId so all bets for a jackpot land on the same partition
        //
        // Using a stable key preserves per-jackpot ordering across partitions and consumers,
        // which simplifies pool update logic and aligns with Kafka's ordering guarantees.
        kafkaTemplate.send(topicName, bet.getJackpotId(), bet);
    }
}
