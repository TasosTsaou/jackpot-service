package com.example.jackpot.strategy.registry;

import com.example.jackpot.model.enums.RewardStrategyType;
import com.example.jackpot.strategy.annotations.RewardType;
import com.example.jackpot.strategy.reward.RewardStrategy;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class RewardStrategyRegistry {
    private final Map<RewardStrategyType, RewardStrategy> registry;

    public RewardStrategyRegistry(List<RewardStrategy> strategies) {
        this.registry = new EnumMap<>(RewardStrategyType.class);
        for (RewardStrategy strategy : strategies) {
            RewardType ann = strategy.getClass().getAnnotation(RewardType.class);
            if (ann != null) {
                RewardStrategyType type = ann.value();
                if (registry.putIfAbsent(type, strategy) != null) {
                    throw new IllegalStateException("Duplicate reward strategy for type: " + type);
                }
            }
        }
    }

    /**
     * Resolves the reward strategy implementation for the given type. A missing mapping is treated
     * as a configuration error and results in an {@link IllegalArgumentException}.
     *
     * @param type the reward strategy type configured on a jackpot
     * @return the matching reward strategy bean
     */
    public RewardStrategy get(RewardStrategyType type) {
        RewardStrategy strategy = registry.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No reward strategy registered for type: " + type);
        }
        return strategy;
    }
}
