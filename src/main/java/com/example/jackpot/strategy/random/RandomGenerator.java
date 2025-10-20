package com.example.jackpot.strategy.random;

/**
 * Abstraction over random number generation so strategies depend on a contract instead
 * of {@link java.util.Random}. This supports easier testing and swaps to deterministic streams.
 */
public interface RandomGenerator {

    /**
     * @return pseudo-random double in the range [0.0, 1.0) used for probability checks.
     */
    double nextDouble();
}
