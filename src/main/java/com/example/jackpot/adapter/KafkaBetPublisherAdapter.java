package com.example.jackpot.adapter;

import com.example.jackpot.kafka.BetProducer;
import com.example.jackpot.model.Bet;
import com.example.jackpot.port.BetPublisher;
import org.springframework.stereotype.Component;

/**
 * Kafka adapter that implements the {@link BetPublisher} port using the existing {@link BetProducer}.
 */
@Component
public class KafkaBetPublisherAdapter implements BetPublisher {

    private final BetProducer producer;

    public KafkaBetPublisherAdapter(BetProducer producer) {
        this.producer = producer;
    }

    @Override
    public void publish(Bet bet) {
        producer.sendBet(bet);
    }
}

