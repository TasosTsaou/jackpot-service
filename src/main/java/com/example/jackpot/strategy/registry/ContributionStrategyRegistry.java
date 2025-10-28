package com.example.jackpot.strategy.registry;

import com.example.jackpot.model.enums.ContributionStrategyType;
import com.example.jackpot.strategy.annotations.ContributionType;
import com.example.jackpot.strategy.contribution.ContributionStrategy;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ContributionStrategyRegistry {
    private final Map<ContributionStrategyType, ContributionStrategy> registry;

    public ContributionStrategyRegistry(List<ContributionStrategy> strategies) {
        this.registry = new EnumMap<>(ContributionStrategyType.class);
        for (ContributionStrategy strategy : strategies) {
            ContributionType ann = strategy.getClass().getAnnotation(ContributionType.class);
            if (ann != null) {
                ContributionStrategyType type = ann.value();
                if (registry.putIfAbsent(type, strategy) != null) {
                    throw new IllegalStateException("Duplicate contribution strategy for type: " + type);
                }
            }
        }
    }

    /**
     * Resolves the contribution strategy implementation for the provided type.
     * Throws {@link IllegalArgumentException} if no strategy has been registered for the type,
     * ensuring misconfiguration fails fast at runtime.
     *
     * @param type the contribution strategy type stored on a jackpot
     * @return the matching strategy implementation
     */
    public ContributionStrategy get(ContributionStrategyType type) {
        ContributionStrategy strategy = registry.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No contribution strategy registered for type: " + type);
        }
        return strategy;
    }
}
