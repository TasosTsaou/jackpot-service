# Adding a New Strategy (Contribution or Reward)

This guide outlines the steps to introduce a new a) contribution strategy and b) reward strategy in this codebase. It follows the existing hexagonal architecture: strategies are pluggable and selected via enums and annotation‑backed registries (no service switches to edit).

## Prerequisites
- Understand current strategies: see `src/main/java/com/example/jackpot/strategy/*`.
- Selection is handled by registries:
  - Contribution: `ContributionStrategyRegistry` maps enum → strategy bean via `@ContributionType`.
  - Reward: `RewardStrategyRegistry` maps enum → strategy bean via `@RewardType`.
- Be ready to extend the `Jackpot` model if your strategy needs config fields.

---

## A) Add a Contribution Strategy

1) Add enum value
- File: `src/main/java/com/example/jackpot/model/enums/ContributionStrategyType.java`
- Add a new enum constant (e.g., `TIERED`).

2) Implement the strategy
- Create: `src/main/java/com/example/jackpot/strategy/contribution/<YourStrategy>.java`
- Implement `ContributionStrategy#calculateContribution(Jackpot jackpot, double betAmount)`
- Annotate with:
  - `@ContributionType(ContributionStrategyType.<YOUR_ENUM>)`
  - `@Component`

3) Registration (no service changes)
- The registry auto‑discovers strategies via the `@ContributionType` annotation. No edits to `JackpotService` are required.
  - Annotations: `src/main/java/com/example/jackpot/strategy/annotations/ContributionType.java`
  - Registry: `src/main/java/com/example/jackpot/strategy/registry/ContributionStrategyRegistry.java`

4) Model/config (if needed)
- File: `src/main/java/com/example/jackpot/model/Jackpot.java`
- Add configuration fields your strategy needs (e.g., thresholds, percentages).
- Seed: update `src/main/resources/data/jackpots-seed.json` to include example values.
- Validation: ensure sane bounds (non-negative, probabilities in [0,1]).

5) Tests
- Unit tests in `src/test/java/com/example/jackpot/strategy/contribution/`
  - Cover boundary values and typical scenarios.
- Optional integration test to verify pool updates when a bet is processed.

6) Docs
- Update `docs/components-description.md` and, if algorithmic, add notes to `docs/jackpot-logic.md`.

---

## B) Add a Reward Strategy

1) Add enum value
- File: `src/main/java/com/example/jackpot/model/enums/RewardStrategyType.java`
- Add a new enum constant (e.g., `CAPPED`).

2) Implement the strategy
- Create: `src/main/java/com/example/jackpot/strategy/reward/<YourStrategy>.java`
- Implement `RewardStrategy#isWinner(Jackpot)` and `#calculateReward(Jackpot)` coherently.
- Annotate with:
  - `@RewardType(RewardStrategyType.<YOUR_ENUM>)`
  - `@Component`
- If randomness is required, inject `RandomGenerator` to keep tests deterministic.

3) Registration (no service changes)
- The registry auto‑discovers strategies via the `@RewardType` annotation. No edits to `RewardService` are required.
  - Annotations: `src/main/java/com/example/jackpot/strategy/annotations/RewardType.java`
  - Registry: `src/main/java/com/example/jackpot/strategy/registry/RewardStrategyRegistry.java`

4) Defaults and model config (if needed)
- File: `src/main/java/com/example/jackpot/model/Jackpot.java`
- Add fields your strategy needs (e.g., base chance, payout curve parameters, caps).
- To externalize environment defaults, extend `src/main/java/com/example/jackpot/config/RewardStrategyProperties.java` with a new nested block and add matching keys in `src/main/resources/application.yml`.
- Seed: update `src/main/resources/data/jackpots-seed.json` to show a jackpot configured with the new strategy.

5) Tests
- Unit tests in `src/test/java/com/example/jackpot/strategy/reward/`
  - Use a stubbed `RandomGenerator` to make win/lose outcomes deterministic.
  - Validate payout calculations and edge cases (0/1 probabilities, empty pools, caps).
- Optional integration test verifying pool reset and reward append on win.

6) Docs
- Update `docs/components-description.md` and, if relevant, `docs/reward-computation-concerns.md` to describe semantics (e.g., payout behavior, fairness).

---

## Strategy Registry Pattern (already implemented)
The codebase already provides annotation‑backed registries, so adding new strategies does not require touching services. See:
- `src/main/java/com/example/jackpot/strategy/annotations/ContributionType.java`
- `src/main/java/com/example/jackpot/strategy/annotations/RewardType.java`
- `src/main/java/com/example/jackpot/strategy/registry/ContributionStrategyRegistry.java`
- `src/main/java/com/example/jackpot/strategy/registry/RewardStrategyRegistry.java`

---

## Quick Checklist
- Enum constant added
- Strategy class implemented + `@Component` + `@ContributionType` or `@RewardType`
- No service changes required (registries auto‑wire)
- `Jackpot` fields extended if needed
- Defaults in properties + `application.yml` (reward only)
- Seed data updated
- Tests added (unit + optional integration)
- Docs updated

---

## Minimal Skeletons

Contribution strategy
```java
@ContributionType(ContributionStrategyType.TIERED)
@Component
public class TieredContributionStrategy implements ContributionStrategy {
  @Override
  public double calculateContribution(Jackpot jackpot, double betAmount) {
    // Example: higher percent for small pools, lower for large pools
    double pool = Math.max(0.0, jackpot.getPoolAmount());
    double percent = pool < 10_000 ? 0.10 : 0.03;
    return betAmount * percent;
  }
}
```

Reward strategy
```java
@RewardType(RewardStrategyType.CAPPED)
@Component
public class CappedRewardStrategy implements RewardStrategy {
  private final RandomGenerator rng;
  public CappedRewardStrategy(RandomGenerator rng) { this.rng = rng; }
  @Override
  public boolean isWinner(Jackpot jackpot) {
    double chance = Math.min(1.0, Math.max(0.0, 0.05 + jackpot.getPoolAmount() / 100_000));
    return rng.nextDouble() < chance;
  }
  @Override
  public double calculateReward(Jackpot jackpot) {
    // Pay min(currentPool, cap)
    double cap = 50_000.0; // consider moving to Jackpot config
    return Math.min(jackpot.getPoolAmount(), cap);
  }
}
```

Refer to existing implementations for style and wiring:
- Contribution: `FixedContributionStrategy`, `VariableContributionStrategy`
- Reward: `FixedRewardStrategy`, `VariableRewardStrategy`

---

## See Also
- Decoupling strategy selection from services (annotation + registry): `docs/decouple-strategies-from-services.md`
