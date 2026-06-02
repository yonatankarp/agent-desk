# AGENTS.md

This repository is public. Treat all tracked files, issues, CI logs, screenshots, and reports as public artifacts.

## Project Defaults

- Kotlin/KMP-first product architecture.
- Desktop and mobile clients are first-class targets.
- Backend/daemon work should use Kotlin unless there is a concrete, documented reason to introduce another runtime.
- OpenClaw integration belongs behind an adapter boundary.
- Do not commit private OpenClaw paths, real Discord channel IDs, tokens, raw transcripts, personal data, or private logs.

## Daily Autonomous Run Contract

Each daily run should:

1. Pull latest `main`.
2. Review open issues, CI, current milestone, `VISION.md`, and recent commits.
3. Choose one narrow, high-leverage unblocked slice.
4. Assign required roles for the slice.
5. Use bounded subagents for role work when useful.
6. Integrate through the main agent.
7. Run available checks.
8. Commit and push coherent changes.
9. Update or close issues with evidence.
10. Post a concise daily report to the configured reporting channel.

Stop or ask when work is blocked by credentials, external setup, product direction, destructive operations, or a decision that should not be guessed.

## Required Role Review

Every non-trivial issue or daily slice must account for these roles:

- Manager: scope, priority, issue hygiene, and acceptance criteria.
- Architect: boundaries, data model, platform choices, and migration risk.
- Developer: implementation and local integration.
- Designer: UX and interaction quality for visible surfaces. Required for UI changes.
- Reviewer/Tester: diff review, tests, failure modes, and regression checks.
- Docs/Operator: runbooks, decision log, daily report, and public-safe artifact hygiene.

Not every role needs a separate agent every day. The orchestrator must decide which roles are required and record the decision.

## Branch Policy

Work should land through small, coherent commits. Once branch protection is enabled, changes to `main` should require passing CI and review according to the repository ruleset.

