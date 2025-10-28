package com.example.jackpot.port;

import com.example.jackpot.model.Reward;
import java.time.Duration;
import java.util.Optional;

/**
 * Outbound port encapsulating reward-by-bet storage and a lightweight evaluation lock.
 */
public interface RewardEvaluationGate {
    Optional<Reward> findRewardByBetId(String betId);
    void saveRewardByBetId(Reward reward);
    boolean tryAcquireRewardLock(String betId, Duration ttl);
}

