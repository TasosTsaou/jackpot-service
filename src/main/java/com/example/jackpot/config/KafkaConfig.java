package com.example.jackpot.config;

import com.example.jackpot.model.Bet;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka producer/consumer configuration for bet events.
 */
@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // --- Producer (Bet) ---
    @Bean
    public ProducerFactory<String, Bet> betProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Avoid adding type headers for cross-language compatibility if needed
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Bet> betKafkaTemplate() {
        return new KafkaTemplate<>(betProducerFactory());
    }

    // --- Consumer (Bet) ---
    @Bean
    public ConsumerFactory<String, Bet> betConsumerFactory(
            @Value("${spring.kafka.consumer.group-id:jackpot-consumers}") String groupId) {

        JsonDeserializer<Bet> jsonDeserializer = new JsonDeserializer<>(Bet.class);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.setUseTypeMapperForKey(false);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Bet> betKafkaListenerContainerFactory(
            ConsumerFactory<String, Bet> betConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Bet> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(betConsumerFactory);
        factory.setConcurrency(1); // dev-friendly; increase for throughput
        return factory;
    }
}
