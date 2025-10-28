package com.example.jackpot.strategy.annotations;

import com.example.jackpot.model.enums.RewardStrategyType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RewardType {
    RewardStrategyType value();
}

