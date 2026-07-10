# Documentation Index

Agent Desk keeps `README.md` short. It should explain what the project is, how to run it locally, and where to find deeper material. Longer architecture, style, process, domain, and CI policy notes live in `docs/`.

## Reading Order

- [Domain model](domain-model.md): public-safe core concepts, value objects, event envelope, and adapter-neutral examples.
- [Agent Desk milestone completion criteria](milestone-completion-criteria.md): first milestone done/not-done states, required evidence, and non-goals.
- [Agent Desk information architecture](information-architecture.md): primary operator surfaces, shared vocabulary, and read-only/actionable boundaries.
- [Runtime adapter boundary](runtime-adapter-boundary.md): sanitized import contracts for local/runtime observations.
- [Observation contract v1](observation-contract-v1.md): public-safe sanitized observation export fields, validation, and diagnostics.
- [Canonical sanitized replay](canonical-sanitized-replay.md): repeatable public-safe replay proof for timeline-ready and decision-queue-ready state.
- [Runtime adapter scope decision](runtime-adapter-scope-decision.md): accepted first non-mock adapter scope and public-safe follow-up boundaries.
- [Live host connectivity milestone](live-host-connectivity-milestone.md): staged diagnostic, read-only sync, and approval-gated action milestones for configured hosts.
- [Local event store](local-event-store.md): JVM-local newline-delimited JSON event persistence.
- [Local audit store](local-audit-store.md): durable append-only persistence for permission decisions and approval outcomes.
- [Local-first smoke suite](local-first-smoke-suite.md): Makefile-backed core loop smoke without external services.
- [Milestone readiness report](milestone-readiness-report.md): release readiness template and checklist.
- [Milestone readiness report - 2026-07-08](milestone-readiness-2026-07-08.md): current public-safe readiness status after the live inspect slices.
- [Milestone readiness report - 2026-07-09](milestone-readiness-2026-07-09.md): not-ready release-candidate readiness pass with green automated gates and a manual UI evidence gap.
- [Milestone readiness report - 2026-07-10](milestone-readiness-2026-07-10.md): ready release-candidate readiness pass with refreshed public-safe manual UI evidence.
- [Manual desktop and mobile UI evidence - 2026-07-10](manual-ui-evidence-2026-07-10.md): dated public-safe desktop and mobile UI evidence for the current release candidate.
- [Runtime configuration](runtime-configuration.md): public-safe mode/source/store configuration contract and mock runtime smoke.
- [Failed local host connection runbook](failed-host-connection-runbook.md): public-safe troubleshooting flow for local-network host connection failures.
- [Action permission gates](action-permission-gates.md): action class inventory and fail-closed permission behavior.
- [Live host action approval](live-host-action-approval-proposal.md): accepted approval and audit flow for the first live inspect action.
- [Public-safe artifact policy](public-safe-artifact-policy.md): artifact classes, sharing rules, and operator recovery runbooks.
- [Privacy boundary regression](privacy-boundary-regression.md): safe persist/commit/publish rules and representative leak fixtures.
- [Roadmap verification gate matrix](roadmap-verification-gate-matrix.md): capability-to-evidence acceptance gates.
- [Verification evidence](verification-evidence.md): structured check results, completion evidence examples, and the pending verification event contract proposal.
- [Digest and notification rules](digest-notification-rules.md): read-only attention rules, urgency, digest grouping, dedupe, and deferred delivery boundaries.
- [Mobile display-parity contract](mobile-read-only-contract.md): shared `:app` read model, display parity expectations, and action boundaries.
- [Engineering style](engineering-style.md): Kotlin, domain, adapter, test, and documentation conventions.
- [Decision log](decision-log.md): durable architecture and process decisions.
- [Dependabot policy](dependabot-policy.md): dependency update policy, auto-merge gates, and coverage gate status.
- [Public hygiene](public-hygiene.md): tracked-file public-safety scanner scope, blocked pattern families, placeholder examples, and local fixture smoke.
- [Desktop verification](desktop-verification.md): headless desktop smoke verification for the current Compose shell.
- [Mobile display shell](mobile-read-only-shell.md): first Compose Multiplatform mobile proof and smoke verification.
- [Branch protection](branch-protection.md): recommended `main` protection and required checks.
- [Role contract](roles.md): role coverage expectations for autonomous slice work, including post-merge Discovery.
- [Daily report template](daily-report-template.md): reporting shape for daily implementation loops and discovery output.
- [Empty backlog discovery checklist](empty-backlog-discovery.md): no-actionable-work audit inputs and report shape.

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
