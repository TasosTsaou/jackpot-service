package com.example.jackpot.repository;

import com.example.jackpot.model.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Low-level Redis data access encapsulating keys, serialization, and atomic
 * updates used by adapters and services.
 */
@Repository
public class RedisRepository {

    private final RedisTemplate<String, Object> redis;

    /**
     * Constructs the repository with the configured Redis template.
     *
     * @param redis template responsible for interacting with Redis
     */
    public RedisRepository(RedisTemplate<String, Object> redis) {
        this.redis = redis;
    }

    /**
     * @param id jackpot identifier
     * @return Redis key used for storing jackpot state
     */
    private String jackpotKey(String id) {
        return "jackpot:" + id;
    }

    /**
     * @param id bet identifier
     * @return Redis key used for storing bet payloads
     */
    private String betKey(String id) {
        return "bet:" + id;
    }

    /**
     * @param id jackpot identifier
     * @return Redis list key storing contributions for the jackpot
     */
    private String contribListKey(String id) {
        return "jackpot:contrib:" + id;
    }

    /**
     * @param id jackpot identifier
     * @return Redis list key storing rewards for the jackpot
     */
    private String rewardListKey(String id) {
        return "jackpot:reward:" + id;
    }

    // --- Key helpers for idempotency and reward-by-bet storage ---
    private String processedBetKey(String id) { return "processed:bet:" + id; }
    private String rewardByBetKey(String id) { return "reward:bybet:" + id; }
    private String rewardLockKey(String id) { return "lock:reward:" + id; }
    private String betProcessingLockKey(String id) { return "lock:betproc:" + id; }

    // --- Bets ---
    /**
     * Persists a bet event so that reward evaluation can reference it later.
     *
     * @param bet bet payload to store
     */
    public void saveBet(Bet bet) {
        redis.opsForHash().put(betKey(bet.getBetId()), "data", bet);
    }

    /**
     * Retrieves a previously stored bet.
     *
     * @param betId bet identifier
     * @return optional containing the bet when found
     */
    public Optional<Bet> findBet(String betId) {
        Object data = redis.opsForHash().get(betKey(betId), "data");
        return data instanceof Bet ? Optional.of((Bet) data) : Optional.empty();
    }

    // --- Jackpots ---
    /**
     * Saves the jackpot snapshot and mirrors the pool amount in a scalar hash entry for atomic updates.
     *
     * @param jackpot jackpot state to persist
     */
    public void saveJackpot(Jackpot jackpot) {
        String key = jackpotKey(jackpot.getJackpotId());
        redis.opsForHash().put(key, "data", jackpot);
        redis.opsForHash().put(key, "poolAmount", jackpot.getPoolAmount());
    }

    /**
     * Looks up jackpot configuration and current state.
     *
     * @param id jackpot identifier
     * @return optional containing the jackpot when present
     */
    public Optional<Jackpot> findJackpotById(String id) {
        Object data = redis.opsForHash().get(jackpotKey(id), "data");
        return data instanceof Jackpot ? Optional.of((Jackpot) data) : Optional.empty();
    }

    /**
     * Adds the supplied delta to the jackpot pool in an atomic Redis operation.
     *
     * @param jackpotId jackpot identifier
     * @param delta     amount to increment
     * @return the updated pool amount
     */
    public double incrementJackpotPool(String jackpotId, double delta) {
        // Atomic increment; returns new value
        return redis.opsForHash().increment(jackpotKey(jackpotId), "poolAmount", delta);
    }

    /**
     * Resets the jackpot pool back to the initial amount, recreating the record if missing.
     *
     * @param jackpotId     jackpot identifier
     * @param initialAmount amount to restore
     */
    public void resetJackpotPool(String jackpotId, double initialAmount) {
        Optional<Jackpot> jackpotOpt = findJackpotById(jackpotId);
        if (jackpotOpt.isPresent()) {
            Jackpot jackpot = jackpotOpt.get();
            jackpot.setPoolAmount(initialAmount);
            saveJackpot(jackpot);
        } else {
            Jackpot jackpot = Jackpot.builder()
                    .jackpotId(jackpotId)
                    .initialAmount(initialAmount)
                    .poolAmount(initialAmount)
                    .build();
            saveJackpot(jackpot);
        }
    }

    // --- Contributions ---
    /**
     * Appends a contribution entry, stamping it with the current time.
     *
     * @param contribution contribution to record
     */
    public void appendContribution(Contribution contribution) {
        contribution.setCreatedAt(Instant.now());
        redis.opsForList().leftPush(contribListKey(contribution.getJackpotId()), contribution);
    }

    // --- Rewards ---
    /**
     * Appends a reward entry, stamping it with the current time so auditing remains consistent.
     *
     * @param reward reward record to store
     */
    public void appendReward(Reward reward) {
        reward.setCreatedAt(Instant.now());
        redis.opsForList().leftPush(rewardListKey(reward.getJackpotId()), reward);
    }

    // --- Idempotency helpers ---
    /**
     * Marks a bet as processed using SETNX with an expiration. Returns true only for the first
     * caller that marks the bet, false for subsequent calls until the TTL elapses.
     */
    public boolean markBetProcessed(String betId) {
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(processedBetKey(betId), 1, java.time.Duration.ofHours(72))
        );
    }

    /**
     * Checks whether a bet has already been marked as processed.
     */
    public boolean isBetProcessed(String betId) {
        Boolean exists = redis.hasKey(processedBetKey(betId));
        return exists != null && exists;
    }

    /**
     * Attempts to acquire a short-lived lock for reward evaluation of a bet.
     * Returns true if acquired, false if another evaluator holds the lock.
     */
    public boolean tryAcquireRewardLock(String betId, String holderId, java.time.Duration ttl) {
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(rewardLockKey(betId), holderId, ttl)
        );
    }

    /**
     * Attempts to acquire a short-lived lock for bet processing.
     */
    public boolean tryAcquireBetProcessing(String betId, String holderId, java.time.Duration ttl) {
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(betProcessingLockKey(betId), holderId, ttl)
        );
    }

    /**
     * Persists a reward record under the bet id for idempotent retrieval.
     */
    public void saveRewardByBetId(Reward reward) {
        redis.opsForHash().put(rewardByBetKey(reward.getBetId()), "data", reward);
    }

    /**
     * Retrieves a reward previously stored by bet id.
     */
    public Optional<Reward> findRewardByBetId(String betId) {
        Object data = redis.opsForHash().get(rewardByBetKey(betId), "data");
        return data instanceof Reward ? Optional.of((Reward) data) : Optional.empty();
    }
}
