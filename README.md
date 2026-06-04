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

The repository is in bootstrap mode. Current slices establish shared core domain types, a sample CLI operator surface, a sample Compose desktop shell, and CI-backed checks.

Start with [docs/index.md](docs/index.md) for deeper architecture, style, process, and domain notes.

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

Run the CLI from sanitized newline-delimited event JSON:

```bash
printf '%s\n' '{"id":"event:agent-task:42:started","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.started","payload":{"title":"Run public hygiene check","summary":"Agent accepted the task and started local checks."}}' | ./gradlew :cli:run --args='--stdin'
```

Or write a public-safe event record to a file and read it back:

```bash
printf '%s\n' '{"id":"event:agent-task:42:started","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.started","payload":{"title":"Run public hygiene check","summary":"Agent accepted the task and started local checks."}}' > agent-desk-events.ndjson
./gradlew :cli:run --args='--events agent-desk-events.ndjson'
```

Inspect one sanitized work item from the same event input:

```bash
./gradlew :cli:run --args='inspect agent-task:42 --events agent-desk-events.ndjson'
```

Run the CLI from a public-safe runtime config:

```bash
printf '%s\n' 'mode=stored-events' 'source=local-event-store' 'eventStoreLocation=agent-desk-events.ndjson' > agent-desk.config.properties
./gradlew :cli:run --args='--config agent-desk.config.properties'
```

Build and test the CLI module:

```bash
./gradlew :cli:build
```

Build the standalone executable CLI jar:

```bash
./gradlew :cli:executableJar
java -jar cli/build/libs/agent-desk-cli-all.jar
```

Run the sample Compose desktop shell:

```bash
./gradlew :desktop:run
```

Build and test the desktop module:

```bash
./gradlew :desktop:build
```

Check deterministic formatting:

```bash
./gradlew spotlessCheck
```

Run architecture convention checks:

```bash
./gradlew :core:jvmTest
```

Generate coverage reports:

```bash
./gradlew :core:koverXmlReport :core:koverHtmlReport
```

Apply deterministic formatting:

```bash
./gradlew spotlessApply
```

Run both checks before opening a PR:

```bash
bash scripts/validate-public-hygiene.sh
./gradlew spotlessCheck
./gradlew :core:build :cli:build :desktop:build
```

## Releases

Releases are created from the `Release` GitHub Actions workflow. Run it manually from `main` and choose the SemVer bump: `patch`, `minor`, or `major`.

The workflow resolves the next `vX.Y.Z` tag from existing tags, runs public hygiene, `spotlessCheck`, builds `:cli:executableJar`, smoke-runs `cli/build/libs/agent-desk-cli-all.jar`, creates the release tag, uploads the jar as an Actions artifact, and attaches the same jar to the GitHub Release.

GitHub generates release notes from merged PRs. Maintainers should label PRs before running a release so notes land in the right sections; use labels such as `feature`, `enhancement`, `slice`, `bug`, `fix`, `decision`, `docs`, `dependencies`, `ci`, `tooling`, `chore`, or `refactor`. Use `no-release-notes` or `skip-release-notes` only for PRs that should be omitted from public notes.

## Public-Safe Rule

Assume every commit, issue, CI log, screenshot, and report is public.

Do not commit private paths, tokens, real channel IDs, raw agent transcripts, personal data, private URLs, or OpenClaw-internal secrets. Use adapters, templates, and sanitized examples for local integrations.

## Dependency Updates

Dependabot policy and auto-merge gates are documented in [docs/dependabot-policy.md](docs/dependabot-policy.md).

## Deeper Docs

- [Documentation index](docs/index.md)
- [Engineering style](docs/engineering-style.md)
- [Domain model](docs/domain-model.md)
- [Decision log](docs/decision-log.md)
