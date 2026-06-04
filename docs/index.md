# Documentation Index

Agent Desk keeps `README.md` short. It should explain what the project is, how to run it locally, and where to find deeper material. Longer architecture, style, process, domain, and CI policy notes live in `docs/`.

## Reading Order

- [Domain model](domain-model.md): public-safe core concepts, value objects, event envelope, and adapter-neutral examples.
- [Runtime adapter boundary](runtime-adapter-boundary.md): sanitized import contracts for local/runtime observations.
- [Local event store](local-event-store.md): JVM-local newline-delimited JSON event persistence.
- [Runtime configuration](runtime-configuration.md): public-safe mode/source/store configuration contract.
- [Mobile read-only contract](mobile-read-only-contract.md): shared `:app` read model and first mobile evidence expectations.
- [Engineering style](engineering-style.md): Kotlin, domain, adapter, test, and documentation conventions.
- [Decision log](decision-log.md): durable architecture and process decisions.
- [Dependabot policy](dependabot-policy.md): dependency update policy, auto-merge gates, and coverage gate status.
- [Public hygiene](public-hygiene.md): tracked-file public-safety scanner scope, allowlist policy, and local fixture smoke.
- [Desktop verification](desktop-verification.md): headless desktop smoke verification for the current Compose shell.
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

Coverage is report-only for now. CI uploads generated `:core`, `:app`, and `:cli` Kover reports as separate artifacts and comments one parsed module summary on same-repo pull requests, but it does not enforce a threshold until the test surface is larger. `:desktop` is excluded from coverage reporting until it has meaningful smoke tests or dedicated testable presentation logic.

Local coverage commands:

```bash
./gradlew :core:koverXmlReport :core:koverHtmlReport
./gradlew :app:koverXmlReport :app:koverHtmlReport
./gradlew :cli:koverXmlReport :cli:koverHtmlReport
```

CI still reuses `yonatankarp/github-actions` for JVM preparation and Gradle builds. The shared actions repository also has a test-report publishing action, but this repo keeps a small local Kover summary/comment script because it needs module-specific coverage parsing, same-repo PR comment updates, and an explicit excluded-module note.
