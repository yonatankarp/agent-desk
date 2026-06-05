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
- [Mobile status](https://github.com/yonatankarp/agent-desk/wiki/Mobile-status)
- [Daily autonomous run reports](https://github.com/yonatankarp/agent-desk/wiki/Daily-autonomous-run-reports)

## Local Checks

Install a local Java/JDK first. The Gradle wrapper needs a launcher JVM available through `JAVA_HOME` or `java` on `PATH` before it can start and use the repository's configured JVM toolchain for builds and run tasks.

Use the root `Makefile` as the preferred local command index:

```bash
make help
```

Run the default local pre-PR checks:

```bash
make check
```

Run all public-safe smoke workflows:

```bash
make smoke
```

Run the local CI-adjacent check set:

```bash
make ci-local
```

Verify coverage thresholds and generate reports:

```bash
make coverage
```

The lower-level shell scripts and Gradle invocations remain available when a target needs a narrower command. Common direct targets include:

```bash
make hygiene
make format-check
make build
make smoke-mock
make smoke-mobile
make smoke-compose
make smoke-sanitized-runtime
```

For CLI examples, runtime configuration, desktop status, and local event store workflows, use the Wiki links above. Repo docs remain canonical for architecture and implementation details.

Build the standalone executable CLI jar:

```bash
make cli-jar
java -jar cli/build/libs/agent-desk-cli-all.jar
```

Run the sample Compose desktop shell:

```bash
make desktop-run
```

Run the sample-only Compose mobile shell:

```bash
make mobile-run
```

Build and test the desktop module:

```bash
./gradlew :desktop:build
```

Apply deterministic formatting:

```bash
make format
```

Run release gates locally without tagging, publishing, or requiring secrets:

```bash
make release-gate
```

## Releases

Releases are created from the `Release` GitHub Actions workflow. Run it manually from `main` and choose the SemVer bump: `patch`, `minor`, or `major`.

The workflow resolves the next `vX.Y.Z` tag from existing tags, runs public hygiene, `spotlessCheck`, CLI tests, mock runtime smoke, builds `:cli:executableJar`, smoke-runs `cli/build/libs/agent-desk-cli-all.jar`, creates the release tag, uploads the jar as an Actions artifact, and attaches the same jar to the GitHub Release. Test and smoke gates run before tag creation, artifact upload, or GitHub Release creation.

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
