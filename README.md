# Jackpot Service

A Spring Boot backend that processes bet events, routes them through Kafka, updates jackpot pools in Redis, and allows clients to evaluate whether a bet wins a jackpot reward. The service uses strategy patterns for contribution and reward logic and ships with Docker Compose tooling for quick local testing.

### Quickstart (TL;DR)

```bash
make test          # run unit & integration tests
make up            # build and start stack
make demo          # run smoke test
make down          # stop and clean up
```

## Table of Contents
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Quickstart (Make)](#quickstart-make)
- [Jackpot seeding](#jackpot-seeding)
- [Manual API smoke test](#manual-api-smoke-test)
- [Automated smoke test](#automated-smoke-test)
- [Gradle tests](#gradle-tests)
- [API Docs](#api-docs)
- [Configuration](#configuration)
- [Key packages](#key-packages)
- [Architecture](#architecture)
- [Docker Compose services](#docker-compose-services)
- [License](#license)

---

## Tech Stack

| Area            | Technology |
|-----------------|------------|
| Language        | Java 17    |
| Framework       | Spring Boot 3 |
| Messaging       | Apache Kafka (KRaft) |
| Data store      | Redis 7 |
| Build Tool      | Gradle |
| Containerisation| Docker & Docker Compose |

---

## Prerequisites

- Java 17+ (only needed if building outside Docker)
- Docker 24+ and Docker Compose plugin
- Bash (for the helper script)

---

## Getting Started

1. **Build jars locally (optional):**
   ```bash
   ./gradlew build
   ```
2. **Start the stack:**
   ```bash
   docker-compose up -d --build
   ```
3. **Wait for services:** Redis and Kafka expose health checks; the application will start once they are healthy. Logs can be viewed with `docker compose logs -f`.
4. **Stop everything:**
   ```bash
   docker compose down
   ```

### Quickstart (Make)

If you have `make` available, these shortcuts wrap common workflows:

```bash
make test              # run unit & integration tests
make up                # build and start stack
make demo              # run smoke test against running stack
make logs              # follow service logs
make down              # stop and remove volumes
make openapi-export    # write docs/api/openapi.json from a running app
```

---

## Jackpot seeding

On boot the application loads `src/main/resources/data/jackpots-seed.json` and writes any missing jackpots to Redis. The shipped file contains a single `J1` jackpot using fixed contribution and reward strategies. Modify or extend the JSON before starting the containers to try different setups.

Example fixed seed entry:
```json
[
  {
    "jackpotId": "J1-FIXED",
    "poolAmount": 1000.0,
    "initialAmount": 1000.0,
    "contributionStrategyType": "FIXED",
    "rewardStrategyType": "FIXED",
    "fixedContributionPercent": 0.05,
    "variableContributionStartPercent": null,
    "variableContributionMinPercent": null,
    "variableContributionDecayPool": null,
    "fixedRewardChance": 0.1,
    "fixedRewardPayoutPercent": 0.8,
    "variableRewardPoolToMaxChance": null
  }
]
```
Example variable seed entry:
```json
[
  {
    "jackpotId": "J2-VARIABLE",
    "poolAmount": 1000.0,
    "initialAmount": 1000.0,
    "contributionStrategyType": "VARIABLE",
    "rewardStrategyType": "VARIABLE",
    "variableContributionStartPercent": 0.10,
    "variableContributionMinPercent": 0.02,
    "variableContributionDecayPool": 2000.0,
    "fixedRewardChance": 0.05,
    "variableRewardPoolToMaxChance": 5000.0
  }
]
```
---

## Manual API smoke test

With the stack running:

1. **POST a bet**
   ```bash
   curl -X POST http://localhost:8080/api/bets \
     -H "Content-Type: application/json" \
     -d '{"betId":"b1","userId":"u1","jackpotId":"J1","betAmount":50.0}'
   ```
   Expected response:
   ```
   Bet published to Kafka: b1
   ```

2. **Evaluate the bet**
   ```bash
   curl http://localhost:8080/api/jackpots/b1/evaluate
   ```
   Example response (most runs will be a non-winning jackpot):
   ```json
   {
     "betId": "b1",
     "userId": "u1",
     "jackpotId": "J1",
     "jackpotRewardAmount": 0.0,
     "createdAt": "2025-10-17T15:55:31.257Z"
   }
   ```

---

## Automated smoke test

This helper script submits 20 bets from different users, captures the evaluation responses, and writes them to `jackpot_output.txt` (one JSON record per line).
The script varies the `userId` for every bet (`cli-user-01`, `cli-user-02`, ...), which makes it easier to trace winnings back to distinct players in Redis.
Once the stack is running, execute from the project root:

```bash
./scripts/e2e_test_place_bets.sh
```
Alternatively, use `make demo` (requires the stack to be up).


---

## Gradle tests

Execute the unit and integration tests locally with:
```bash
./gradlew test
```

### Terminal reporting
- Per-test events are printed in the console (passed, skipped, failed).
- Failures show full stack traces and causes.
- A final one-line summary is printed, for example:
  - `Test Summary: SUCCESS | Total: 42, Passed: 42, Failed: 0, Skipped: 0`

### HTML report
- Full report: `build/reports/tests/test/index.html`
- Class-specific pages live under `build/reports/tests/test/classes/`


## API Docs

- Live docs: browse `http://localhost:8080/swagger-ui` once the app is running.
- Raw OpenAPI JSON: `http://localhost:8080/v3/api-docs`.
- Export to the repo: `make openapi-export` (writes `docs/api/openapi.json`).

Notes:
- Endpoints are available under both `/api` and `/api/v1` for compatibility.
- Error responses use RFC 7807 (`application/problem+json`).


## Configuration

Reward strategy defaults live under `reward.strategy` in `application.yml`. Override
`fixed.default-win-probability`, `fixed.default-payout-percent`, and the variable strategy
defaults to tweak baseline behavior without code changes.
The fixed and variable reward strategies inject these settings plus a pluggable random source, keeping the implementations aligned with SOLID principles.

Redis serialization registers Jackson's Java Time module so `Instant` fields persist correctly; if you supply a custom `RedisTemplate`, ensure the module stays registered.

Note for Windows users: helper scripts in `scripts/` are Bash-based. Use Git Bash or WSL, or run the equivalent Gradle/Compose commands directly.

---

## Key packages

- `controller` – REST API endpoints for submitting bets and evaluating jackpots.
- `kafka` – Producer and consumer wiring for the `jackpot-bets` topic.
- `service` – Business logic orchestrating contributions and reward evaluations.
- `strategy` – Fixed and variable contribution/reward strategy implementations.
- `repository` – Redis access layer for bets, jackpots, contributions, and rewards.
- `port` – Outbound interfaces (e.g., BetStore, BetPublisher, JackpotQuery/Store) used by services.
- `adapter` – Concrete implementations of ports for Kafka/Redis.
- `bootstrap` – `JackpotSeeder` that loads seed data on startup.

---

## Architecture

This project follows a hexagonal (ports & adapters) structure with a light read/write split:

- Services depend on ports (interfaces) and are decoupled from Kafka/Redis adapters.
- Business rules are implemented via strategies (fixed/variable).
- Reward defaults are configuration‑driven.

Read the full overview: `docs/architecture-overview.md`

### Jackpot Logic

A walkthrough is presented here on the reasoning about how the pool grows, how wins are decided, and what’s configurable:

- `docs/jackpot-logic.md`

---

## Docker Compose services

| Service | Purpose | Ports |
|---------|---------|-------|
| `app`   | Spring Boot service | 8080 |
| `kafka` | Apache Kafka broker (KRaft) | 9092 |
| `redis` | Redis in-memory store | 6379 |

All services expose health checks that the helper scripts and manual workflow rely on.

---

## License

This project is released under the MIT License. See [`LICENSE`](LICENSE) for details.
