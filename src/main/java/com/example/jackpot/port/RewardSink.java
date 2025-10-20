package com.example.jackpot.port;

import com.example.jackpot.model.Reward;

/**
 * Outbound port for recording reward events.
 */
public interface RewardSink {
    void appendReward(Reward reward);
}

