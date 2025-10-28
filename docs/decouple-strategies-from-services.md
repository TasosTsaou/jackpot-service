# Decoupling Strategies From Services

Goal: Add new strategies without editing `JackpotService` or `RewardService`.

## Problem
Both services contain `switch` statements that hard‑code strategy types. Every new strategy forces code changes and redeploys in the application layer.

## Recommendation (Annotation + Registry)
Introduce a small registry per strategy family that maps enum → strategy bean at startup. Each strategy self‑declares which enum it serves via a lightweight annotation. Services ask the registry for the strategy by enum, removing `switch` logic.

### Design
- Annotations
  - `@ContributionType(ContributionStrategyType)` on contribution strategies
  - `@RewardType(RewardStrategyType)` on reward strategies
- Registries
  - `ContributionStrategyRegistry` and `RewardStrategyRegistry` collect all strategy beans, read the annotations, and build an `EnumMap<…>`.
  - Validate uniqueness; fail fast on duplicate or missing mappings.
- Services
  - Replace `switch`/direct fields with a single dependency on the registry and a `get(type)` lookup.

### Benefits
- Zero edits to services for new strategies
- Single place for validation and discovery
- Works with Spring DI and existing tests

## Alternatives
1) Bean‑name map: name beans as enum names and inject `Map<String, Strategy>`. Simple but relies on naming discipline.
2) Self‑describing strategies: add `getType()` to the interface. Requires one interface change; no annotations.
3) Factory interface: return strategies by type. More boilerplate; useful for dynamic construction.
4) Java SPI/ServiceLoader: suitable for plugin architectures outside Spring; overkill here.

## Rollout Steps
1) Add annotations and registries under `com.example.jackpot.strategy.annotations` and `…registry` packages.
2) Annotate existing strategies (fixed/variable) with their types.
3) Update services to depend on registries and remove `switch` logic.
4) Run tests and adjust as needed.

## Usage Example
```java
// Service code
ContributionStrategy strategy = contributionRegistry.get(jackpot.getContributionStrategyType());
double contribution = strategy.calculateContribution(jackpot, betAmount);
```

```java
// Strategy declaration
@ContributionType(ContributionStrategyType.FIXED)
@Component
class FixedContributionStrategy implements ContributionStrategy { … }
```
