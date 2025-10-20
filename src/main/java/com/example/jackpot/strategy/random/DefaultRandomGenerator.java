package com.example.jackpot.strategy.random;

import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Production-grade random generator that delegates to {@link Random} while keeping the seed
 * encapsulated. The indirection allows swapping in deterministic generators for tests.
 */
@Component
public class DefaultRandomGenerator implements RandomGenerator {

    private final Random random;

    /**
     * Creates the generator with a time-based seed to avoid predictable sequences in production.
     */
    public DefaultRandomGenerator() {
        this(new Random());
    }

    /**
     * Secondary constructor for wiring explicit {@link Random} instances, primarily used in tests.
     *
     * @param random backing random instance
     */
    DefaultRandomGenerator(Random random) {
        this.random = random;
    }

    /**
     * Generates the next pseudo-random value from the underlying {@link Random}.
     *
     * @return value in the range [0.0, 1.0)
     */
    @Override
    public double nextDouble() {
        return random.nextDouble();
    }
}
