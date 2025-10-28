# Idempotency Strategy

This document describes how to achieve end-to-end idempotency for the jackpot service when ingesting bets via Kafka and persisting state in Redis. The goal is to make all side effects safe under at-least-once delivery and retries.

## Goals
- Prevent duplicate side effects from producer or consumer retries.
- Preserve per‑jackpot ordering while scaling horizontally.
- Use atomic operations to avoid lost updates under concurrency.

## Overview
- Kafka producer idempotence reduces duplicates originating from the producer.
- Consumer processing remains at‑least‑once; enforce idempotency with a dedup gate keyed by `betId` and use atomic Redis updates.
- Reward evaluation is idempotent by `betId`, protected by a lightweight lock to ensure single computation, and stored so repeated calls return the same result.

## Kafka Producer Idempotence
Enable producer idempotence for safer retries and ordering:

- File: `src/main/java/com/example/jackpot/config/KafkaConfig.java`
  - In the producer properties, add:
    - `ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG = true`
    - `ProducerConfig.RETRIES_CONFIG = Integer.MAX_VALUE`
    - `ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION = 5`
  - Optional throughput tuning: `linger.ms`, `batch.size`.

The app already keys records by `jackpotId` (see `src/main/java/com/example/jackpot/kafka/BetProducer.java`) to preserve per‑jackpot ordering across partitions.

## Kafka Transactions (wired)
While Redis effects remain idempotent/outside Kafka, the app wires Kafka transactions so that, when producing from listeners, output records and input offset commits can be committed atomically.

- Producer factory enables transactions and idempotence, with a `transactionIdPrefix`:
  - `src/main/java/com/example/jackpot/config/KafkaConfig.java:49`
- Transaction manager bean and transactional listener container:
  - `src/main/java/com/example/jackpot/config/KafkaConfig.java:90`, `:98`
- Consumers use `read_committed` isolation to only see committed transactional data:
  - `src/main/java/com/example/jackpot/config/KafkaConfig.java:79`

Usage
- In `@KafkaListener`s, using `KafkaTemplate` will participate in the transaction managed by the container. For producer‑only atomic sends, wrap in `kafkaTemplate.executeInTransaction(...)`.

## Consumer-Side Exactly-Once Effects
Because Redis is external to Kafka, treat consumption as at‑least‑once and make the effects idempotent. The app implements this via hexagonal ports so services do not depend on Redis directly.

### 1) Deduplicate Processing by betId (via port)
- Port: `BetProcessingGate` handles idempotence and a short processing lock.
- On POST ingestion (`BetService`): check `isBetProcessed(betId)` and accept idempotently if true, without re‑publishing.
  - File: `src/main/java/com/example/jackpot/service/BetService.java:1`
- On consume (`JackpotService`):
  - If `isBetProcessed(betId)` is true, skip.
  - Otherwise acquire a short‑lived processing token `tryAcquireProcessing(betId, ttl≈10s)`; if not acquired, skip (another worker is processing).
  - After all side effects succeed, call `markBetProcessed(betId)`.
  - File: `src/main/java/com/example/jackpot/service/JackpotService.java:1`
  - Port interface: `src/main/java/com/example/jackpot/port/BetProcessingGate.java:1`
  - Redis adapter: `src/main/java/com/example/jackpot/adapter/RedisBetProcessingGateAdapter.java:1`

### 2) Atomic Jackpot Pool Updates (via port)
- Port: `JackpotPool.incrementPool(jackpotId, delta)` performs an atomic add and returns the new pool.
- Service usage: `JackpotService` calls the port instead of read‑modify‑write.
  - Files: `src/main/java/com/example/jackpot/service/JackpotService.java:1`, `src/main/java/com/example/jackpot/port/JackpotPool.java:1`
  - Redis adapter: `src/main/java/com/example/jackpot/adapter/RedisJackpotPoolAdapter.java:1` (uses `HINCRBYFLOAT`).

### 3) Contribution History
- After passing the dedup gate, append the contribution record once. If strict idempotency is required for the list, store a reference keyed by `betId` and only push if absent.

## Reward Evaluation Idempotency
Ensure that evaluating a reward for a bet is a single, repeatable operation. Implemented via a dedicated port so services remain storage‑agnostic.

### Storage by betId (via port)
- Port: `RewardEvaluationGate` provides:
  - `findRewardByBetId(betId)` — return stored result if present.
  - `saveRewardByBetId(reward)` — persist once for idempotency (win or no‑win).
  - `tryAcquireRewardLock(betId, ttl)` — short lock to avoid duplicate evaluation under races.
- Service usage: `RewardService` first checks stored result; if absent, acquires lock, computes a result, stores it by bet, appends to history, and resets the pool.
  - Files: `src/main/java/com/example/jackpot/service/RewardService.java:1`, `src/main/java/com/example/jackpot/port/RewardEvaluationGate.java:1`
  - Redis adapter: `src/main/java/com/example/jackpot/adapter/RedisRewardEvaluationGateAdapter.java:1`

### Single Evaluation Lock
- The `RewardEvaluationGate` exposes `tryAcquireRewardLock`. If the lock is not acquired, callers read the stored result or return a retriable error.

### Atomic Pool Reset on Win
- Use `JackpotPool.incrementPool(jackpotId, delta)` with `delta = initialAmount - currentPool` to reset atomically, then persist the snapshot via `JackpotStore`.

## Redis Key Schema (Suggested)
- `jackpot:{jackpotId}` — hash
  - fields: `data` (object), `poolAmount` (double)
- `bet:{betId}` — hash
  - fields: `data` (object)
- `processed:bet:{betId}` — string (presence flag)
- `jackpot:contrib:{jackpotId}` — list of `Contribution`
- `jackpot:reward:{jackpotId}` — list of `Reward`
- `reward:bybet:{betId}` — hash of `Reward`
- `lock:reward:{betId}` — ephemeral lock key

## Implementation Touchpoints
- Producer idempotence and keying:
  - `src/main/java/com/example/jackpot/config/KafkaConfig.java`, `src/main/java/com/example/jackpot/kafka/BetProducer.java`
- Bet processing idempotency and atomic pool updates (hexagonal):
  - Ports: `BetProcessingGate`, `JackpotPool`
  - Adapters: `RedisBetProcessingGateAdapter`, `RedisJackpotPoolAdapter`
  - Service: `src/main/java/com/example/jackpot/service/JackpotService.java`
- Reward idempotency and locking (hexagonal):
  - Port: `RewardEvaluationGate`
  - Adapter: `RedisRewardEvaluationGateAdapter`
  - Service: `src/main/java/com/example/jackpot/service/RewardService.java`

## Example Flow: processBet (hexagonal)
Pseudocode for the core idempotent path using ports:

```
if (processingGate.isBetProcessed(betId)) return;
if (!processingGate.tryAcquireProcessing(betId, 10s)) return;

Jackpot j = jackpotQuery.findJackpotById(jackpotId).orElseThrow();
double contribution = contributionStrategy.calculateContribution(j, betAmount);
double newPool = jackpotPool.incrementPool(jackpotId, contribution);

j.setPoolAmount(newPool);
jackpotStore.saveJackpot(j); // persist snapshot
contributionSink.appendContribution(record);
processingGate.markBetProcessed(betId); // only after success
```

## Testing Scenarios
- Duplicate deliveries: publish same `betId` twice and verify only one contribution update occurs.
- Concurrency: run multiple consumers; assert final pool equals sum of contributions.
- Reward idempotency: call evaluateReward twice for the same bet; verify same result and single reset.

## Operations
- Monitor Kafka consumer lag and partition skew; scale concurrency up to the partition count.
- Track Redis ops latency and memory; set TTLs for dedup and lock keys (e.g., 24–72h for `processed:bet:*`, 10–30s for locks).
- Consider disabling broker auto‑create and let the app manage topic/partitions for consistency.

## Notes
- Per‑jackpot ordering is preserved by keying to `jackpotId`; increasing partitions raises parallelism across jackpots but does not interleave events for the same jackpot.
- Kafka transactions are useful when producing to other Kafka topics atomically; for Redis side effects, prefer dedup + atomic operations as above.
