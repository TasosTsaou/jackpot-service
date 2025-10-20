package com.example.jackpot.bootstrap;

import com.example.jackpot.model.Jackpot;
import com.example.jackpot.repository.RedisRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Bootstraps initial jackpot definitions from a JSON seed file at startup.
 */
@Component
public class JackpotSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(JackpotSeeder.class);

    private final RedisRepository redisRepository;
    private final ObjectMapper objectMapper;
    private final Resource seedResource;

    public JackpotSeeder(RedisRepository redisRepository,
                         ObjectMapper objectMapper,
                         @Value("classpath:data/jackpots-seed.json") Resource seedResource) {
        this.redisRepository = redisRepository;
        this.objectMapper = objectMapper;
        this.seedResource = seedResource;
    }

    @Override
    public void run(String... args) {
        List<Jackpot> jackpots = readSeedData();
        if (jackpots.isEmpty()) {
            log.info("No jackpot seed data found – skipping bootstrap.");
            return;
        }

        for (Jackpot jackpot : jackpots) {
            String jackpotId = jackpot.getJackpotId();
            if (jackpotId == null || jackpotId.isBlank()) {
                log.warn("Skipping seeded jackpot with missing id: {}", jackpot);
                continue;
            }

            normalizeAmounts(jackpot);

            try {
                Optional<Jackpot> existing = redisRepository.findJackpotById(jackpotId);
                if (existing.isPresent()) {
                    log.debug("Jackpot {} already present in Redis – not overwriting.", jackpotId);
                    continue;
                }

                redisRepository.saveJackpot(jackpot);
                log.info("Seeded jackpot {} with initial pool {}", jackpotId, jackpot.getPoolAmount());
            } catch (DataAccessException e) {
                log.error("Unable to seed jackpot {}: {}", jackpotId, e.getMessage());
            }
        }
    }

    private List<Jackpot> readSeedData() {
        if (seedResource == null || !seedResource.exists()) {
            return Collections.emptyList();
        }

        try (InputStream inputStream = seedResource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<List<Jackpot>>() {});
        } catch (IOException e) {
            log.error("Failed to read jackpot seed data: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private void normalizeAmounts(Jackpot jackpot) {
        if (jackpot.getInitialAmount() <= 0 && jackpot.getPoolAmount() > 0) {
            jackpot.setInitialAmount(jackpot.getPoolAmount());
        } else if (jackpot.getInitialAmount() > 0 && jackpot.getPoolAmount() <= 0) {
            jackpot.setPoolAmount(jackpot.getInitialAmount());
        }
    }
}
