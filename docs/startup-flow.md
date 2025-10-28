# Jackpot Service Startup and Runtime Flow

This document walks through the application lifecycle from startup to request handling and background processing. File references include line numbers for quick navigation.

## Startup
- Entry point: `SpringApplication.run` builds the Spring context and starts the app.
  - `src/main/java/com/example/jackpot/JackpotServiceApplication.java:11`
- Component scanning (base package `com.example.jackpot`) detects:
  - Configuration beans: Redis, Kafka, properties binding.
  - Adapters: Redis and Kafka implementations of outbound ports.
  - Services: domain orchestration services.
  - Strategies: contribution and reward logic, random generator.
  - Web: REST controller, exception handler.
  - Messaging: Kafka producer/consumer.
  - Bootstrap: data seeder.

## Bean Wiring (Configuration)
- Redis configuration:
  - Connection factory via Lettuce with `spring.redis.*` from `application.yml`.
    - `src/main/java/com/example/jackpot/config/RedisConfig.java:20`
  - `RedisTemplate<String, Object>` with Jackson JSON serializer, Java Time module, and polymorphic typing.
    - `src/main/java/com/example/jackpot/config/RedisConfig.java:27`
- Kafka configuration:
  - Producer: `ProducerFactory<String, Bet>` + `KafkaTemplate<String, Bet>` with JSON serializer.
    - `src/main/java/com/example/jackpot/config/KafkaConfig.java:21`
  - Consumer: `ConsumerFactory<String, Bet>` + `ConcurrentKafkaListenerContainerFactory`, group and bootstrap from config.
    - `src/main/java/com/example/jackpot/config/KafkaConfig.java:31`
  - Kafka listener processing enabled by `@EnableKafka`.
    - `src/main/java/com/example/jackpot/config/KafkaConfig.java:11`
- Reward strategy properties binding from `application.yml` (`reward.strategy.*`).
  - `src/main/java/com/example/jackpot/config/RewardStrategyProperties.java:11`

## Bootstrap Seeding (on startup)
- `JackpotSeeder` implements `CommandLineRunner`; `run(...)` executes after context initialization.
  - `src/main/java/com/example/jackpot/bootstrap/JackpotSeeder.java:41`
- Constructed with `RedisRepository`, `ObjectMapper`, and `classpath:data/jackpots-seed.json`.
  - `src/main/java/com/example/jackpot/bootstrap/JackpotSeeder.java:33`
- Flow:
  - Parse seed JSON into `List<Jackpot>`; if missing/empty, log and return.
    - `src/main/java/com/example/jackpot/bootstrap/JackpotSeeder.java:43`, `:73`, `:78`
  - For each jackpot:
    - Validate `jackpotId` is non-blank; skip if invalid.
      - `src/main/java/com/example/jackpot/bootstrap/JackpotSeeder.java:50`
    - Normalize amounts: if one of `initialAmount`/`poolAmount` is zero/absent but the other is set, copy the value.
      - `src/main/java/com/example/jackpot/bootstrap/JackpotSeeder.java:86`
    - If not present in Redis, save it and log; otherwise skip.
      - Check: `:59`
      - Save: `:65`
- Note: Two log messages contain odd characters (encoding artifact).
  - `src/main/java/com/example/jackpot/bootstrap/JackpotSeeder.java:45`, `:61`

## HTTP Entry Points
- Base paths: `/api`, `/api/v1`
  - `src/main/java/com/example/jackpot/controller/BetController.java:22`
- Submit Bet: `POST /bets`
  - Validates body and calls `BetService.handleBet`.
  - `src/main/java/com/example/jackpot/controller/BetController.java:33`, `:57`
- Evaluate Outcome: `GET /jackpots/{betId}/evaluate`
  - Calls `RewardService.evaluateReward` and returns `Reward`.
  - `src/main/java/com/example/jackpot/controller/BetController.java:60`, `:74`
- Global exception mapping to RFC7807 Problem Details for validation and argument errors.
  - `src/main/java/com/example/jackpot/controller/GlobalExceptionHandler.java:15`

## Bet Ingestion Flow (POST /bets)
- `BetService.handleBet` persists the bet and publishes it to Kafka.
  - `src/main/java/com/example/jackpot/service/BetService.java:26`
- Persist:
  - `RedisBetStoreAdapter.save` → `RedisRepository.saveBet` saves `hash bet:{betId}["data"] = Bet`.
  - `src/main/java/com/example/jackpot/adapter/RedisBetStoreAdapter.java:22`
  - `src/main/java/com/example/repository/RedisRepository.java:66`
- Publish:
  - `KafkaBetPublisherAdapter.publish` → `BetProducer.sendBet` → `KafkaTemplate.send("jackpot-bets", betId, bet)`.
  - `src/main/java/com/example/jackpot/adapter/KafkaBetPublisherAdapter.java:20`
  - `src/main/java/com/example/jackpot/kafka/BetProducer.java:16`

## Asynchronous Processing (Kafka Consumer)
- `BetConsumer.consumeBet` listens to `jackpot-bets` and forwards to the service.
  - `src/main/java/com/example/jackpot/kafka/BetConsumer.java:19`
- `JackpotService.processBet` orchestrates contribution and state updates.
  - `src/main/java/com/example/jackpot/service/JackpotService.java:43`
- Steps:
  - Load jackpot by `bet.jackpotId` via Redis.
    - `src/main/java/com/example/jackpot/adapter/RedisJackpotQueryAdapter.java:23`
    - `src/main/java/com/example/repository/RedisRepository.java:115`
  - If missing, log and return.
    - `src/main/java/com/example/jackpot/service/JackpotService.java:45`
  - Select `ContributionStrategy` by `ContributionStrategyType`:
    - Fixed: percent from jackpot override or default 5%.
      - `src/main/java/com/example/jackpot/strategy/contribution/FixedContributionStrategy.java:12`
    - Variable: decays percent from start to min as pool grows, up to a decay horizon.
      - `src/main/java/com/example/jackpot/strategy/contribution/VariableContributionStrategy.java:20`
  - Compute contribution = `betAmount * percent`, update pool, persist jackpot.
    - Update: `src/main/java/com/example/jackpot/service/JackpotService.java:54`
    - Persist: `src/main/java/com/example/jackpot/adapter/RedisJackpotStoreAdapter.java:22`
    - `src/main/java/com/example/repository/RedisRepository.java:87`
  - Append `Contribution` audit record with timestamp.
    - `src/main/java/com/example/jackpot/service/JackpotService.java:58`
    - `src/main/java/com/example/jackpot/adapter/RedisContributionSinkAdapter.java:22`
    - `src/main/java/com/example/repository/RedisRepository.java:144`

## Reward Evaluation Flow (GET /jackpots/{betId}/evaluate)
- `RewardService.evaluateReward(betId)` executes the evaluation.
  - `src/main/java/com/example/jackpot/service/RewardService.java:54`
- Steps:
  - Load bet; if missing, throw `IllegalArgumentException` → HTTP 400.
    - `src/main/java/com/example/jackpot/service/RewardService.java:56`
  - Load jackpot by `bet.jackpotId`; if missing, throw `IllegalArgumentException`.
    - `src/main/java/com/example/jackpot/service/RewardService.java:62`
  - Select `RewardStrategy` by `RewardStrategyType`.
    - `src/main/java/com/example/jackpot/service/RewardService.java:96`
  - If winner:
    - Compute `rewardAmount` using strategy.
    - Build `Reward` with `createdAt = Instant.now()` and append to Redis audit list.
      - Build/append: `src/main/java/com/example/jackpot/service/RewardService.java:71`, `:79`
      - Adapter: `src/main/java/com/example/jackpot/adapter/RedisRewardSinkAdapter.java:22`
      - Repository: `src/main/java/com/example/repository/RedisRepository.java:155`
    - Reset jackpot pool to `initialAmount`, persist, return `Reward`.
      - `src/main/java/com/example/jackpot/service/RewardService.java:83`, `:86`
  - If not a winner:
    - Return `Reward` with `jackpotRewardAmount = 0.0` and timestamp (not persisted).
      - `src/main/java/com/example/jackpot/service/RewardService.java:88`

## Strategy Logic
- Randomness abstraction:
  - `RandomGenerator` interface and `DefaultRandomGenerator` implementation for testability and decoupling from `java.util.Random`.
  - `src/main/java/com/example/jackpot/strategy/random/DefaultRandomGenerator.java:11`
- Fixed reward:
  - Winner check: probability from jackpot override or default, clamped to [0,1], compare to `random.nextDouble()`.
    - `src/main/java/com/example/jackpot/strategy/reward/FixedRewardStrategy.java:40`
  - Payout: fixed percentage of current pool, percent ≥ 0.
    - `src/main/java/com/example/jackpot/strategy/reward/FixedRewardStrategy.java:57`
- Variable reward:
  - Winner check: probability increases from base to 100% as pool approaches a configured cap.
    - `src/main/java/com/example/jackpot/strategy/reward/VariableRewardStrategy.java:39`
  - Payout: awards the full pool.
    - `src/main/java/com/example/jackpot/strategy/reward/VariableRewardStrategy.java:65`

## Data Access (RedisRepository)
- Key patterns:
  - Jackpot hash: `jackpot:{id}` with fields `data` (object) and `poolAmount` (scalar).
    - `src/main/java/com/example/repository/RedisRepository.java:32`, `:87`
  - Bet hash: `bet:{id}` with field `data`.
    - `src/main/java/com/example/repository/RedisRepository.java:40`, `:66`
  - Contribution list: `jackpot:contrib:{id}`; Reward list: `jackpot:reward:{id}`.
    - `src/main/java/com/example/repository/RedisRepository.java:52`, `:60`
- Convenience operations:
  - `incrementJackpotPool`, `resetJackpotPool` available (not directly used by services).
    - `src/main/java/com/example/repository/RedisRepository.java:124`

## Kafka Wiring
- Producer sends `Bet` events to topic `jackpot-bets` using key `betId`.
  - `src/main/java/com/example/jackpot/kafka/BetProducer.java:16`
- Consumer group `jackpot-consumers` processes the same topic; concurrency = 1 by default.
  - `src/main/java/com/example/jackpot/config/KafkaConfig.java:49`
  - `src/main/java/com/example/jackpot/kafka/BetConsumer.java:19`

## Error Handling
- `GlobalExceptionHandler` maps:
  - `IllegalArgumentException` → 400 Invalid request.
  - `MethodArgumentNotValidException` → 400 Validation failed.
  - `ConstraintViolationException` → 400 Constraint violation.
  - `src/main/java/com/example/jackpot/controller/GlobalExceptionHandler.java:15`

## Putting It Together
- App starts and seeds jackpots from JSON.
  - `src/main/resources/data/jackpots-seed.json:1`
- Clients POST bets → bet stored in Redis and published to Kafka.
- Consumer processes bets → calculates contributions and updates jackpot pool → writes contribution audits.
- Clients GET evaluation → load bet/jackpot → strategy decides win → writes reward and resets pool if win; otherwise returns zero‑payout result.

## Configuration Reference
- Application config:
  - `src/main/resources/application.yml:1`
  - Kafka bootstrap servers, consumer group, Redis host/port, reward strategy defaults.

## Seed Data
- Two jackpots provided:
  - `J1-FIXED`: fixed contribution 5%, fixed reward chance 10%, payout 80% of pool.
  - `J2-VARIABLE`: variable contribution (10% → 2% over pool growth), variable reward (base 5%, 100% chance at pool ≥ 5000).
  - `src/main/resources/data/jackpots-seed.json:1`
