# Scaling Jackpot Service For Concurrency

This guide details how to scale bet ingestion and reward evaluation safely under concurrency, with concrete code touchpoints and operational guidance.

## Goals
- High throughput bet ingestion across many instances.
- Preserve per-jackpot ordering without global locks.
- Avoid race conditions on jackpot pool updates and reward resets.
- Idempotent reward evaluation with safe retries.

## Architecture Levers
- Kafka partitioning and consumer concurrency for horizontal scale.
- Redis atomic operations (increments, optional locks) for consistency.
- Stateless services behind a load balancer.

## Kafka: Partitioning and Concurrency
- Key by jackpotId to preserve per-jackpot ordering across partitions.
  - File: `src/main/java/com/example/jackpot/kafka/BetProducer.java:16`
    - Current: `kafkaTemplate.send("jackpot-bets", bet.getBetId(), bet)`
    - Change to: `kafkaTemplate.send("jackpot-bets", bet.getJackpotId(), bet)`
- Increase partitions for parallelism (e.g., 12–24+ depending on QPS). Ensure total consumer threads ≤ partitions.
- Scale consumers horizontally:
  - Run N app instances in the same consumer group (e.g., `jackpot-consumers`).
  - Increase container concurrency per instance.
    - File: `src/main/java/com/example/jackpot/config/KafkaConfig.java:49`
      - Example: `factory.setConcurrency(4)` (tune with partitions and replicas)
- Improve producer safety/throughput (optional):
  - Idempotent producer: set `enable.idempotence=true` and `max.in.flight.requests.per.connection=5`.
  - Batch more: tune `linger.ms`, `batch.size`.

## Redis: Atomic Pool Updates
- Replace read-modify-write with atomic increment to avoid lost updates.
  - Service: `src/main/java/com/example/jackpot/service/JackpotService.java:54`
    - Current: `newPool = jackpot.getPoolAmount() + contribution; saveJackpot(jackpot)`
    - Change: `double newPool = redisRepository.incrementJackpotPool(jackpotId, contribution); jackpot.setPoolAmount(newPool);`
  - Repository method exists: `src/main/java/com/example/repository/RedisRepository.java:124`
- For complex multi-key updates, prefer Lua scripts or `WATCH/MULTI/EXEC`.

## Reward Evaluation: Idempotency and Locking
- Idempotency by betId:
  - Before computing, check if a reward for `betId` exists and return it if present.
  - Persist results under a dedicated key (e.g., `reward:{betId}`) in addition to the per-jackpot list for history.
  - Extend repository with:
    - `Optional<Reward> findRewardByBetId(String betId)`
    - `void saveRewardByBetId(Reward reward)`
- Single evaluation per bet:
  - Acquire a lightweight distributed lock keyed by betId:
    - `SETNX lock:reward:{betId} <nodeId> EX 10`
    - Proceed only if acquired; release on completion or let TTL expire.
  - Library option: Redisson `RLock` (if adding dependencies is acceptable).
- Atomic pool reset on win:
  - Compute delta to reset pool to initialAmount atomically:
    - `double current = (double) redis.hget(jackpot:{id}, "poolAmount")`
    - `double delta = jackpot.initialAmount - current`
    - `incrementJackpotPool(jackpotId, delta)`
  - Persist the full `Jackpot` snapshot afterward if needed for other fields.

## Ordering Strategies For Strict Consistency (Optional)
- Serialize reward evaluations with contributions by processing both as Kafka commands keyed by `jackpotId`.
  - HTTP `evaluate` enqueues a command (e.g., topic `jackpot-commands`).
  - The same consumer group that processes bets also processes evaluations, guaranteeing order per jackpot without extra locks.
  - The HTTP layer can poll Redis or expose a GET to retrieve the evaluation result by `betId`.

## Data Model Idempotency
- Contributions:
  - Current LPUSH may duplicate under reprocessing. If strict idempotency is required, also store a hash per bet (e.g., `contrib:{betId}`) and only LPUSH a reference if absent.
  - Alternative: switch to Redis Streams with explicit IDs.
- Rewards:
  - Store by betId (hash) and also LPUSH to the per-jackpot list for history.

## Operational Practices
- Kafka
  - Create topic with sufficient partitions and replication.
  - Monitor consumer lag, rebalances, partition skew; add a DLQ for poison messages.
- Redis
  - Use pooled Lettuce connections (already configured). Monitor latency and memory.
  - Set TTLs for locks and dedup keys to bound memory.
- App
  - Autoscale on CPU/QPS or Kafka lag; keep services stateless.
  - Health/readiness checks include Kafka and Redis connectivity.

## Concrete Code Touchpoints
- Producer key change:
  - `src/main/java/com/example/jackpot/kafka/BetProducer.java:16`
- Consumer concurrency:
  - `src/main/java/com/example/jackpot/config/KafkaConfig.java:49`
- Atomic pool increment in service:
  - `src/main/java/com/example/jackpot/service/JackpotService.java:54`
- Reward idempotency + lock + atomic reset:
  - Add repository methods and update `src/main/java/com/example/jackpot/service/RewardService.java:54` flow accordingly.

## Suggested Next Steps
- I can submit diffs to:
  - Change the Kafka key to `jackpotId` and bump listener concurrency.
  - Switch pool updates to `incrementJackpotPool`.
  - Add reward-by-betId storage, simple Redis lock, and atomic reset logic.
