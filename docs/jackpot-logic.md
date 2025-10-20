# Jackpot Logic Explained

This guide explains jackpots like you’ve never seen them before, from what the pool is to how winning works and what you can configure.

---

## Big Picture
- A jackpot is a shared pot of money (the pool).
- Every bet adds a small slice to the pool (a contribution).
- Sometimes a bet wins and takes a prize from the pool (a reward).

## Pool Funding (Contributions)
- The pool starts at an initial seed amount.
- Each bet contributes a percentage of the bet to the pool via one of two strategies:
  - Fixed contribution: always the same percent (e.g., 5% of each bet).
  - Variable contribution: starts higher and gradually decreases as the pool grows (so early bets boost the pool faster).

## What Happens When You Place a Bet
- You send a bet to the system.
- The system stores the bet and publishes it to a background queue.
- A background worker reads the bet, calculates the contribution using the configured strategy, and increases the pool accordingly.

## How Winning Works (Reward Chance)
- You (or a client) later ask: did this bet win the jackpot?
- The system looks at the bet and the jackpot’s settings, then checks a win probability from one of two strategies:
  - Fixed reward: every bet has the same chance (e.g., 10%) to win, regardless of pool size.
  - Variable reward: the chance starts at a base value and ramps up toward 100% as the pool approaches a target size (bigger pool → higher chance).

## If You Win, What You Get
- Fixed reward payout: a set percentage of the current pool (e.g., 80% of the pool).
- Variable reward payout: the entire pool.
- After a win, the pool resets back to its initial seed amount so the cycle restarts.

## If You Don’t Win
- The system records a “no win” event (amount = 0) for audit/history.
- The pool remains larger by your contribution, which can make future wins bigger.

## What You Can Configure (no code needed)
- Contribution
  - Fixed percent (e.g., 0.05 = 5%).
  - Variable start/min percents and how quickly it decays as the pool grows.
- Reward
  - Fixed win chance (e.g., 0.1 = 10%).
  - Fixed payout percent (e.g., 0.8 = 80% of pool).
  - Variable “pool to max chance” (how big the pool must get before wins are certain).
- Defaults are set in `application.yml`, and each jackpot can override them.

## Simple Example
- Seed: 1,000. Bet: 50. Fixed contribution 5% → adds 2.50 to the pool.
- Fixed reward: 10% chance to win. If it wins and payout is 80%, prize = 80% of the current pool; then the pool resets to 1,000.
- If it doesn’t win, the pool stays larger by your contribution, growing the potential future prize.

