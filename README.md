# Agent Desk

Agent Desk is a public-safe, local-first supervisor console for delegated AI work.

The product goal is simple: open the app after time away, understand what agents are doing, see what needs a decision, and drill into evidence quickly.

This repository is Kotlin/KMP-first:

- Kotlin shared core for domain models, event schemas, reducers, and sync/state logic.
- Compose Multiplatform desktop and mobile clients backed by shared Kotlin logic.
- A backend or local daemon can be Kotlin/Ktor when needed.
- OpenClaw is treated as one integration source behind an adapter boundary.

## Current Status

The repository is in bootstrap mode. Current slices establish shared core domain types, a sample CLI operator surface, sample Compose desktop/mobile shells, a public-safe mock runtime smoke, and CI-backed checks.

Start with [docs/index.md](docs/index.md) for deeper architecture, style, process, and domain notes.

## Operator Handbook

The GitHub Wiki is the operator-facing handbook:

- [Quickstart](https://github.com/yonatankarp/agent-desk/wiki/Quickstart)
- [CLI usage](https://github.com/yonatankarp/agent-desk/wiki/CLI-usage)
- [Runtime configuration](https://github.com/yonatankarp/agent-desk/wiki/Runtime-configuration)
- [Local event stores](https://github.com/yonatankarp/agent-desk/wiki/Local-event-stores)
- [Public-safe rules](https://github.com/yonatankarp/agent-desk/wiki/Public-safe-rules)
- [Desktop status](https://github.com/yonatankarp/agent-desk/wiki/Desktop-status)
- [Daily autonomous run reports](https://github.com/yonatankarp/agent-desk/wiki/Daily-autonomous-run-reports)

## Local Checks

Run the public-safe repository hygiene check:

```bash
bash scripts/validate-public-hygiene.sh
```

Run the public-safe mock runtime/operator smoke:

```bash
bash scripts/mock-runtime-smoke.sh
```

Run the public-safe mobile read-only smoke:

```bash
bash scripts/mobile-read-only-smoke.sh
```

Check deterministic formatting:

```bash
./gradlew spotlessCheck
```

Build the current modules:

```bash
./gradlew :core:build :app:build :cli:build :desktop:build :mobile:build
```

For CLI examples, runtime configuration, desktop status, and local event store workflows, use the Wiki links above. Repo docs remain canonical for architecture and implementation details.

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

Generate coverage reports:

```bash
./gradlew :core:koverXmlReport :core:koverHtmlReport
./gradlew :app:koverXmlReport :app:koverHtmlReport
./gradlew :cli:koverXmlReport :cli:koverHtmlReport
./gradlew :desktop:koverXmlReport :desktop:koverHtmlReport
./gradlew :mobile:koverXmlReport :mobile:koverHtmlReport
```

Apply deterministic formatting:

```bash
./gradlew spotlessApply
```

Run both checks before opening a PR:

```bash
bash scripts/validate-public-hygiene.sh
./gradlew spotlessCheck
./gradlew :core:build :app:build :cli:build :desktop:build :mobile:build
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
