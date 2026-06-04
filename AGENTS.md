# AGENTS.md

This repository is public. Treat all tracked files, issues, CI logs, screenshots, and reports as public artifacts.

## Project Defaults

- Kotlin/KMP-first product architecture.
- Desktop and mobile clients are first-class targets.
- Backend/daemon work should use Kotlin unless there is a concrete, documented reason to introduce another runtime.
- OpenClaw integration belongs behind an adapter boundary.
- Prefer reusable workflows and composite actions from `yonatankarp/github-actions` when they fit the slice.
- Do not commit private OpenClaw paths, real Discord channel IDs, tokens, raw transcripts, personal data, or private logs.

## Daily Autonomous Run Contract

Each daily run should:

1. Pull latest `main`.
2. Review open issues, CI, current milestone, `VISION.md`, recent commits, and the state of any local worktrees/workspaces before selecting work.
3. Choose one narrow, high-leverage open issue. Prefer unblocked `slice` issues. If the only actionable open issue is labeled `decision`, work the decision issue as the slice. Record and merge the decision only when the issue or owner comments already contain explicit owner-approved direction. Otherwise open a proposal PR or comment with the recommended decision, keep the issue open, and stop for owner review.
4. Assign required roles for the slice.
5. Use bounded subagents for role work when useful.
6. Integrate through the main agent.
7. Run available checks.
8. Commit and push coherent changes.
9. Update or close issues with evidence.
10. Run a post-merge or post-review discovery audit before selecting the next slice or stopping.
11. Post a concise daily report to the configured reporting channel.

Stop or ask when work is blocked by credentials, external setup, product direction, destructive operations, or a decision that should not be guessed.

Do not infer product direction from weak wording such as "expected", "default", "unless evidence favors", or local implementation convenience. Treat those as recommendation inputs, not approval. For product or architecture choices, create follow-up implementation slices only after the decision is merged or explicitly approved.

Do not start or resume work for a closed issue unless the owner explicitly asks for a rerun. If local worktrees or workspaces exist for closed issues, report them as stale local state and ignore them for work selection. Do not let stale workspaces outrank the live GitHub issue backlog.

## Post-Merge Discovery Audit

After every merged or reviewed slice, run a short discovery audit before moving on. The audit should inspect:

- the original issue acceptance criteria and whether any gap remains
- the final PR diff, tests, CI checks, docs impact, and user-facing behavior
- CodeRabbit findings or review comments when available
- package/module boundaries, suspicious dependencies, TODO/FIXME notes, and recently touched areas
- docs claims compared with the code shape
- tests that are skipped, thin, missing around new behavior, or newly expensive

If CodeRabbit is unavailable, rate-limited, skipped, or silent, record that explicitly and continue using local and GitHub evidence. Create follow-up issues only for real gaps, product opportunities, missing verification, docs drift, architecture risks, or process failures. Each follow-up issue should include Goal, Acceptance Criteria, and Verification. Do not create noisy issues for every thought.

Every final loop report must include discovery output: either the new issues created or a short reason why no follow-up issues were warranted. If the backlog is empty or blocked, spend remaining time on this discovery/codebase scan instead of idling.

Discovery is not a replacement for the main backlog. If open decision issues remain, the next loop should resolve the highest-leverage decision before inventing unrelated implementation issues. If no follow-up issues are created because the only remaining work is a decision issue, say that explicitly in the report and make that decision issue the next focus.

## Required Role Review

Every non-trivial issue or daily slice must account for these roles:

- Manager: scope, priority, issue hygiene, and acceptance criteria.
- Architect: boundaries, data model, platform choices, and migration risk.
- Developer: implementation and local integration.
- QA/Tester: verification evidence, failure modes, and regression checks.
- Compliance: acceptance criteria, engineering style, architecture rules, public-safety rules, and user constraints.
- Security: secrets, privacy, permissions, adapters, persistence, and public-safe boundaries.
- Designer: UX and interaction quality for visible surfaces. Required for UI changes.
- Reviewer: code, design, or implementation review separate from QA evidence.
- Docs/Operator: runbooks, decision log, daily report, and public-safe artifact hygiene.
- Discovery: post-merge audit, CodeRabbit/review findings, codebase scan, and follow-up issue quality.

Not every role needs a separate agent every day. The orchestrator must decide which roles are required and record the decision.

## Branch Policy

Work should land through small, coherent commits. Once branch protection is enabled, changes to `main` should require passing CI and review according to the repository ruleset.

When creating a branch, use a release-note-aware prefix so PR labels can be inferred automatically:

- `feat/...` or `feature/...` for user-facing additions.
- `fix/...` or `bugfix/...` for bug fixes.
- `docs/...` for documentation-only changes.
- `ci/...` for GitHub Actions and automation.
- `build/...` for Gradle, packaging, and build configuration.
- `tooling/...` for developer scripts and repository automation.
- `arch/...` or `architecture/...` for architecture and boundary decisions.
- `refactor/...` for internal refactors without intended behavior changes.
- `chore/...` or `maintenance/...` for routine maintenance.
- `breaking/...` or `break/...` for breaking user-facing or API changes.

Prefer the narrowest accurate prefix. If a branch spans multiple concerns, choose the primary release-note category and rely on path-based PR labeling for secondary labels.

PR titles should stay human-readable, but start them with the same intent when it reads naturally, for example `feat:`, `fix:`, `docs:`, `ci:`, `build:`, `refactor:`, or `chore:`. Branch prefixes are the automation source of truth; PR title prefixes are reviewer-facing context.

## GitHub Actions Reuse

Before adding or changing CI workflows, inspect `yonatankarp/github-actions` for an applicable reusable workflow or composite action.

Known useful candidates:

- `.github/workflows/ci.yml`
- `.github/workflows/linters.yml`
- `.github/workflows/dependabot-auto-merge.yml`
- `.github/actions/gradle-build/action.yml`
- `.github/actions/prepare-jvm-build/action.yml`
- `.github/actions/publish-test-reports/action.yml`

Only use shared workflows when the repository has the expected project shape and the workflow can pass cleanly. Keep a small local workflow when a bootstrap slice needs checks that are not covered by the shared repo yet.
