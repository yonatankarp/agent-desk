# Documentation Index

Agent Desk keeps `README.md` short. It should explain what the project is, how to run it locally, and where to find deeper material. Longer architecture, style, process, domain, and CI policy notes live in `docs/`.

## Reading Order

- [Domain model](domain-model.md): public-safe core concepts, value objects, event envelope, and adapter-neutral examples.
- [Runtime adapter boundary](runtime-adapter-boundary.md): sanitized import contracts for local/runtime observations.
- [Runtime adapter scope decision](runtime-adapter-scope-decision.md): accepted first non-mock adapter scope and public-safe follow-up boundaries.
- [Local event store](local-event-store.md): JVM-local newline-delimited JSON event persistence.
- [Runtime configuration](runtime-configuration.md): public-safe mode/source/store configuration contract and mock runtime smoke.
- [Mobile read-only contract](mobile-read-only-contract.md): shared `:app` read model and first mobile evidence expectations.
- [Engineering style](engineering-style.md): Kotlin, domain, adapter, test, and documentation conventions.
- [Decision log](decision-log.md): durable architecture and process decisions.
- [Dependabot policy](dependabot-policy.md): dependency update policy, auto-merge gates, and coverage gate status.
- [Public hygiene](public-hygiene.md): tracked-file public-safety scanner scope, blocked pattern families, placeholder examples, and local fixture smoke.
- [Desktop verification](desktop-verification.md): headless desktop smoke verification for the current Compose shell.
- [Mobile read-only shell](mobile-read-only-shell.md): first Compose Multiplatform mobile proof and smoke verification.
- [Branch protection](branch-protection.md): recommended `main` protection and required checks.
- [Role contract](roles.md): role coverage expectations for autonomous slice work, including post-merge Discovery.
- [Daily report template](daily-report-template.md): reporting shape for daily implementation loops and discovery output.

## Boundary Rules

- Keep quickstart commands and the public-safe rule in `README.md`.
- Put architecture rationale, domain details, CI policy, role/process guidance, and longer examples in `docs/`.
- Keep the GitHub Wiki as the human-facing operator handbook. When repo docs change operator-facing behavior, update the matching wiki page in the same slice or create a follow-up issue.
- Keep docs public-safe: no private paths, tokens, channel IDs, raw transcripts, personal data, private URLs, or OpenClaw-internal secrets.
- Prefer links between focused docs over copying the same explanation into multiple files.

## Current CI Notes

Konsist rules currently keep production declarations under `com.yonatankarp.agentdesk`, keep `:core` common code under `com.yonatankarp.agentdesk.core.domain`, keep shared `:app` common code under `com.yonatankarp.agentdesk.app`, and prevent core/app production files from importing adapter, CLI, desktop, or UI packages.

Coverage is enforced by module-level Kover line thresholds and still publishes reports. Initial thresholds are intentionally conservative relative to the June 2026 baseline: `:core`, `:app`, `:desktop`, and `:mobile` require 90% line coverage; `:cli` requires 80% line coverage. Raise thresholds only when a merged slice lifts the real baseline, and lower them only with an explicit PR note that explains the coverage loss and recovery plan.

Local coverage commands:

```bash
make coverage
```

CI still reuses `yonatankarp/github-actions` for JVM preparation and Gradle builds. The Ubuntu jobs run hygiene, formatting, full module builds, coverage threshold verification, coverage report publishing, and same-repo PR coverage comments. Dedicated macOS and Windows jobs build the Compose desktop and mobile modules so OS-specific Compose/JVM issues are caught before local desktop use. The shared actions repository also has a test-report publishing action, but this repo keeps a small local Kover summary/comment script because it needs module-specific coverage parsing and same-repo PR comment updates.
