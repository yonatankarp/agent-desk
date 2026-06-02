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

Run the sample CLI operator surface:

```bash
./gradlew :cli:run
```

Build and test the CLI module:

```bash
./gradlew :cli:build
```

Check deterministic formatting:

```bash
./gradlew spotlessCheck
```

Run architecture convention checks:

```bash
./gradlew :core:jvmTest
```

The current Konsist rules keep production declarations under `com.yonatankarp.agentdesk`, keep `:core` common code in the core package, and prevent core production files from importing adapter, desktop, or UI packages.

Generate coverage reports:

```bash
./gradlew :core:koverXmlReport :core:koverHtmlReport
```

Coverage is report-only for now. CI uploads the generated `:core` coverage reports as an artifact, but does not enforce a threshold until the domain test surface is larger.

Apply deterministic formatting:

```bash
./gradlew spotlessApply
```

Run both checks before opening a PR:

```bash
bash scripts/validate-public-hygiene.sh
./gradlew spotlessCheck
./gradlew :core:build :cli:build
```

## Public-Safe Rule

Assume every commit, issue, CI log, screenshot, and report is public.

Do not commit private paths, tokens, real channel IDs, raw agent transcripts, personal data, private URLs, or OpenClaw-internal secrets. Use adapters, templates, and sanitized examples for local integrations.

## Dependency Updates

Dependabot policy and auto-merge gates are documented in [docs/dependabot-policy.md](docs/dependabot-policy.md).
