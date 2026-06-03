# Documentation Index

Agent Desk keeps `README.md` short. It should explain what the project is, how to run it locally, and where to find deeper material. Longer architecture, style, process, domain, and CI policy notes live in `docs/`.

## Reading Order

- [Domain model](domain-model.md): public-safe core concepts, value objects, event envelope, and adapter-neutral examples.
- [Runtime adapter boundary](runtime-adapter-boundary.md): sanitized import contracts for local/runtime observations.
- [Local event store](local-event-store.md): JVM-local newline-delimited JSON event persistence.
- [Engineering style](engineering-style.md): Kotlin, domain, adapter, test, and documentation conventions.
- [Decision log](decision-log.md): durable architecture and process decisions.
- [Dependabot policy](dependabot-policy.md): dependency update policy, auto-merge gates, and coverage gate status.
- [Branch protection](branch-protection.md): recommended `main` protection and required checks.
- [Role contract](roles.md): role coverage expectations for autonomous slice work.
- [Daily report template](daily-report-template.md): reporting shape for daily implementation loops.

## Boundary Rules

- Keep quickstart commands and the public-safe rule in `README.md`.
- Put architecture rationale, domain details, CI policy, role/process guidance, and longer examples in `docs/`.
- Keep docs public-safe: no private paths, tokens, channel IDs, raw transcripts, personal data, private URLs, or OpenClaw-internal secrets.
- Prefer links between focused docs over copying the same explanation into multiple files.

## Current CI Notes

Konsist rules currently keep production declarations under `com.yonatankarp.agentdesk`, keep `:core` common code under `com.yonatankarp.agentdesk.core.domain`, keep shared `:app` common code under `com.yonatankarp.agentdesk.app`, and prevent core/app production files from importing adapter, CLI, desktop, or UI packages.

Coverage is report-only for now. CI uploads generated `:core` coverage reports as an artifact and comments the parsed summary on pull requests, but it does not enforce a threshold until the domain test surface is larger.
