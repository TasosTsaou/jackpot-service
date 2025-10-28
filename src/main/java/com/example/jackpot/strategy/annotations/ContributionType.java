package com.example.jackpot.strategy.annotations;

import com.example.jackpot.model.enums.ContributionStrategyType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ContributionType {
    ContributionStrategyType value();
}

