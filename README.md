# Agent Desk

Agent Desk is a public-safe, local-first supervisor console for delegated AI work.

The product goal is simple: open the app after time away, understand what agents are doing, see what needs a decision, and drill into evidence quickly.

This repository is Kotlin/KMP-first:

- Kotlin shared core for domain models, event schemas, reducers, and sync/state logic.
- Compose Multiplatform desktop as the primary client target.
- iOS support through a native shell backed by shared Kotlin logic, unless product evidence later favors Compose iOS.
- A backend or local daemon can be Kotlin/Ktor when needed.
- OpenClaw is treated as one integration source behind an adapter boundary.

## Current Status

The repository is in bootstrap mode. The first product code slice establishes a Kotlin Multiplatform `:core` module with a small domain value object and CI-backed Gradle tests.

Core tests use Kotest on the shared KMP test source set so domain rules can use spec-style tests and richer assertions as the model grows.

## Local Checks

Run the public-safe repository hygiene check:

```bash
bash scripts/validate-public-hygiene.sh
```

Run the Kotlin core build and tests:

```bash
./gradlew :core:build
```

Check deterministic formatting:

```bash
./gradlew spotlessCheck
```

Apply deterministic formatting:

```bash
./gradlew spotlessApply
```

Run both checks before opening a PR:

```bash
bash scripts/validate-public-hygiene.sh
./gradlew spotlessCheck
./gradlew :core:build
```

## Public-Safe Rule

Assume every commit, issue, CI log, screenshot, and report is public.

Do not commit private paths, tokens, real channel IDs, raw agent transcripts, personal data, private URLs, or OpenClaw-internal secrets. Use adapters, templates, and sanitized examples for local integrations.
