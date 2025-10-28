# Reward Computation Concerns

This note compares when to compute a bet’s reward and how to make it safe and predictable in production.

## Short Answer
- Compute on query: generally no. Reads become non‑idempotent and abusable; timing affects fairness.
- Compute on placement/consume: yes. Decide once during the write path and make reads return stored results.

## Compute on Query (Lazy)
Pros
- Defers work until requested; simple for demos.

Cons
- Read causes side effects (e.g., pool reset) → non‑idempotent GET.
- “Query until you win” risk unless you lock and persist first result by `betId`.
- Outcome tied to query time, not placement time → fairness/compliance issues.
- Racy under concurrency; multiple readers can try to compute.

If you must keep it
- Persist first result under `reward:bybet:{betId}` and always return that.
- Guard with a short‑lived lock `lock:reward:{betId}` to ensure single computation.
- Avoid pool mutations in GET; use POST for evaluation or move to async processing.

## Compute on Placement / On Consume (Recommended)
Pros
- Clear separation: writes decide; reads are side‑effect‑free.
- Deterministic and auditable: outcome bound to placement time and pool state.
- Easy idempotency: dedup by `betId`; queries are fast cache hits.

How to apply here
- Decide reward when the bet is consumed from Kafka (partitioning by `jackpotId` preserves per‑jackpot order).
- Persist `Reward` once per `betId` and append to per‑jackpot history.
- Reset the pool atomically on win, then persist the updated jackpot.
- Make GET return the stored reward; if not ready, respond 202 and let clients poll.

## Idempotency and Concurrency Notes
See `docs/idempotency.md` for details; key points below:
- Dedup gate: `SETNX processed:bet:{betId} 1 EX <ttl>` so each bet is processed once.
- Reward storage: store by bet `reward:bybet:{betId}`, and append to `jackpot:reward:{jackpotId}` for history.
- Single evaluation lock: `SET lock:reward:{betId} <nodeId> NX EX 10` to avoid double computation.
- Atomic pool changes: use an atomic increment; for reset, compute `delta = initialAmount - currentPool` and increment by `delta`.

## API Semantics
- Keep `GET /jackpots/{betId}/evaluate` read‑only: return the stored `Reward`.
  - 200 with reward when available; 202 Accepted if processing is still underway.
- If evaluation must be triggerable, expose a POST/command endpoint instead of mutating on GET.

## Recommendation for This Codebase
- Evaluation is moved to the async path (Kafka consumer) alongside contribution processing and implemented with the existing `RewardService` (no extra processor class).
- Persist the computed reward by `betId` and reset pool in the write path.
- Keep GET primarily read-only; it returns the stored result when available and remains idempotent.

## Implementation Status (current code)
- Consumer orchestration
  - File: `src/main/java/com/example/jackpot/kafka/BetConsumer.java`
  - Flow: after `jackpotService.processBet(bet)`, the consumer calls `rewardService.evaluateReward(bet.getBetId())` to compute and persist rewards idempotently.
- Reward logic
  - File: `src/main/java/com/example/jackpot/service/RewardService.java`
  - Uses ports to apply SOLID/hexagonal principles:
    - `BetQuery`, `JackpotQuery`, `JackpotStore` for reads/writes
    - `RewardStrategyRegistry` to select strategy (OCP)
    - `RewardEvaluationGate` to cache reward by bet and guard with a short lock
    - `JackpotPool` to atomically reset the pool on win
- GET endpoint
  - File: `src/main/java/com/example/jackpot/controller/BetController.java`
  - GET `/jackpots/{betId}/evaluate` leverages `RewardService`. Since compute now happens on consume, GET typically returns the stored reward; if not present, the service still performs an idempotent compute guarded by a short-lived lock.
