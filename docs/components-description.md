# Components and Models

A concise, interview‑level guide to the main models and components: what each is and what it’s used for.

## Models
- Bet — Incoming bet payload (betId, userId, jackpotId, amount). Used for persistence, Kafka events, and reward evaluation. (`src/main/java/com/example/jackpot/model/Bet.java`)
- Contribution — Audit record of how a bet contributes to a jackpot pool; captures stake, computed contribution, resulting pool, timestamp. (`src/main/java/com/example/jackpot/model/Contribution.java`)
- Jackpot — Core jackpot state (current pool, initial seed) and strategy configuration knobs (contribution/reward). Consumed by strategies and persisted in Redis. (`src/main/java/com/example/jackpot/model/Jackpot.java`)
- Reward — Result of evaluating a bet (payout amount or zero) with timestamp; used for responses and auditing. (`src/main/java/com/example/jackpot/model/Reward.java`)
- ContributionStrategyType — Enum selecting contribution strategy (FIXED/VARIABLE). (`src/main/java/com/example/jackpot/model/enums/ContributionStrategyType.java`)
- RewardStrategyType — Enum selecting reward strategy (FIXED/VARIABLE). (`src/main/java/com/example/jackpot/model/enums/RewardStrategyType.java`)

## Ports (Hexagonal Interfaces)
- BetPublisher — Outbound port to publish bets to a messaging system (e.g., Kafka). (`src/main/java/com/example/jackpot/port/BetPublisher.java`)
- BetStore — Outbound port to persist bet payloads. (`src/main/java/com/example/jackpot/port/BetStore.java`)
- BetQuery — Outbound port to read bets by id. (`src/main/java/com/example/jackpot/port/BetQuery.java`)
- JackpotStore — Outbound port to persist jackpot state. (`src/main/java/com/example/jackpot/port/JackpotStore.java`)
- JackpotQuery — Outbound port to read jackpots by id. (`src/main/java/com/example/jackpot/port/JackpotQuery.java`)
- ContributionSink — Outbound port to append contribution records. (`src/main/java/com/example/jackpot/port/ContributionSink.java`)
- RewardSink — Outbound port to append reward records. (`src/main/java/com/example/jackpot/port/RewardSink.java`)

## Adapters (Port Implementations)
- KafkaBetPublisherAdapter — Implements BetPublisher using the Kafka producer. (`src/main/java/com/example/jackpot/adapter/KafkaBetPublisherAdapter.java`)
- RedisBetStoreAdapter — Implements BetStore on Redis; saves bet payloads. (`src/main/java/com/example/jackpot/adapter/RedisBetStoreAdapter.java`)
- RedisBetQueryAdapter — Implements BetQuery on Redis; retrieves bets by id. (`src/main/java/com/example/jackpot/adapter/RedisBetQueryAdapter.java`)
- RedisJackpotStoreAdapter — Implements JackpotStore on Redis; persists jackpot snapshots and pool. (`src/main/java/com/example/jackpot/adapter/RedisJackpotStoreAdapter.java`)
- RedisJackpotQueryAdapter — Implements JackpotQuery on Redis; reads jackpot state. (`src/main/java/com/example/jackpot/adapter/RedisJackpotQueryAdapter.java`)
- RedisContributionSinkAdapter — Implements ContributionSink on Redis; appends contribution entries. (`src/main/java/com/example/jackpot/adapter/RedisContributionSinkAdapter.java`)
- RedisRewardSinkAdapter — Implements RewardSink on Redis; appends reward entries. (`src/main/java/com/example/jackpot/adapter/RedisRewardSinkAdapter.java`)

## Repository
- RedisRepository — Low‑level Redis data access (keys, serialization, atomic ops like increment and list appends). Backing store for adapters, services, and seeding. (`src/main/java/com/example/repository/RedisRepository.java`)

## Services (Application Layer)
- BetService — Persists incoming bets and publishes them via the publisher port. (`src/main/java/com/example/jackpot/service/BetService.java`)
- JackpotService — Consumes bets, computes contribution via strategy, updates jackpot pool, persists state, and records contribution. (`src/main/java/com/example/jackpot/service/JackpotService.java`)
- RewardService — Evaluates whether a bet wins using configured reward strategy; records reward and resets pool on win. (`src/main/java/com/example/jackpot/service/RewardService.java`)

## Strategies
- ContributionStrategy — Interface to calculate pool contribution from a bet. (`src/main/java/com/example/jackpot/strategy/contribution/ContributionStrategy.java`)
- FixedContributionStrategy — Fixed percentage of bet amount contributes to the pool. (`src/main/java/com/example/jackpot/strategy/contribution/FixedContributionStrategy.java`)
- VariableContributionStrategy — Contribution percent decays from a start value to a minimum as the pool grows. (`src/main/java/com/example/jackpot/strategy/contribution/VariableContributionStrategy.java`)
- RewardStrategy — Interface for win check and payout calculation. (`src/main/java/com/example/jackpot/strategy/reward/RewardStrategy.java`)
- FixedRewardStrategy — Constant win probability; pays a percentage of the current pool. (`src/main/java/com/example/jackpot/strategy/reward/FixedRewardStrategy.java`)
- VariableRewardStrategy — Win chance grows with pool; pays out the full pool amount. (`src/main/java/com/example/jackpot/strategy/reward/VariableRewardStrategy.java`)
- RandomGenerator — Abstraction for randomness to keep strategies testable; DefaultRandomGenerator is the production implementation. (`src/main/java/com/example/jackpot/strategy/random/RandomGenerator.java`, `src/main/java/com/example/jackpot/strategy/random/DefaultRandomGenerator.java`)

## Kafka
- BetProducer — Sends bets to the Kafka topic (keyed by jackpotId for partition locality). (`src/main/java/com/example/jackpot/kafka/BetProducer.java`)
- BetConsumer — Listens to the bet topic and delegates processing to JackpotService. (`src/main/java/com/example/jackpot/kafka/BetConsumer.java`)

## Configuration
- KafkaConfig — Producer/consumer factories, listener container factory, and topic creation. (`src/main/java/com/example/jackpot/config/KafkaConfig.java`)
- RedisConfig — Redis connection factory and JSON‑serializing RedisTemplate. (`src/main/java/com/example/jackpot/config/RedisConfig.java`)
- RewardStrategyProperties — Binds tunable defaults for reward strategies (overridable via application.yml). (`src/main/java/com/example/jackpot/config/RewardStrategyProperties.java`)
- application.yml — Environment wiring for Kafka, Redis, and reward defaults. (`src/main/resources/application.yml`)

## Web Layer
- BetController — REST endpoints to submit bets and evaluate rewards. (`src/main/java/com/example/jackpot/controller/BetController.java`)
- GlobalExceptionHandler — Maps common errors to RFC 7807 Problem Details responses. (`src/main/java/com/example/jackpot/controller/GlobalExceptionHandler.java`)

## Bootstrap & Entry Point
- JackpotSeeder — Loads initial jackpots from JSON seed into Redis at startup. (`src/main/java/com/example/jackpot/bootstrap/JackpotSeeder.java`)
- JackpotServiceApplication — Spring Boot main application entry point. (`src/main/java/com/example/jackpot/JackpotServiceApplication.java`)

