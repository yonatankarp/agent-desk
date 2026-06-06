# Roadmap Verification Gate Matrix

This matrix maps Agent Desk capabilities to the evidence required before a
slice can be called accepted or release-ready. It separates CI-required gates
from optional/manual operator checks so local-first work stays fast without
letting risky paths ship silently.

## Gate Types

- unit tests: deterministic domain, projection, parser, policy, and formatter tests
- integration tests: adapter, import, persistence, replay, CLI, or cross-module tests
- smoke tests: runnable CLI, desktop, mobile, or mock-runtime checks that prove the workflow opens and renders
- coverage: module coverage threshold and coverage comment artifacts
- manual QA: operator inspection, screenshot review, or public-safe artifact review
- security review: negative leakage tests, permission checks, public-safe review, and external side-effect review
- design review: required only for UI or interaction changes

## CI-Required Gates

Every PR must pass:

- Repo Hygiene
- Formatting
- Gradle Build
- Coverage
- relevant platform builds when touched: desktop, mobile, CLI, or shared app/core
- `bash scripts/validate-public-hygiene.sh`

CI-required failures must produce actionable diagnostics: failing task/check
name, failing fixture or route when known, and a public-safe summary. Silent
skips, missing artifacts, or empty failure messages are not release-ready.

## Capability Matrix

| Capability | Unit | Integration | Smoke | Coverage | Manual QA | Security Review | Design Review | Required Evidence |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Core event model and public-safe value objects | required | optional | optional | required | optional | required | not required | parser/value tests, public-safe rejection tests, coverage |
| Runtime observation import | required | required | optional | required | optional | required | not required | mapper/import tests, duplicate/store diagnostics, unsafe source rejection |
| Local event store and persistence | required | required | optional | required | optional | required | not required | encode/decode tests, corrupt/duplicate diagnostics, public-safe store errors |
| CLI operator console | required | required | required | required | optional | required | not required | CLI tests, stdin/file failure tests, sample command smoke |
| Desktop operator surface | required | required | required | required | required | required | required | snapshot/compose smoke, evidence visibility, screenshot/manual QA when UI changes |
| Mobile read-only surface | required | required | required | required | required | required | required | mobile snapshot/contract tests, read-only proof, manual device/screenshot QA when UI changes |
| Action capability proposals | required | optional | optional | required | optional | required | not required | proposal-only tests, disabled external/destructive cases, no executor proof |
| Mock action approval loop | required | required | optional | required | optional | required | not required | approval/reject/defer/cancel tests, audit receipts, no external side effects |
| Permission gates | required | required | optional | required | optional | required | not required | action class inventory, deny/approve/cancel/ambiguous/unsupported tests |
| Audit trail | required | required | optional | required | optional | required | not required | timeline/detail projection tests, public-safe actor/action/detail checks |
| Verification evidence | required | optional | optional | required | optional | required | not required | checklist/readiness tests, stale/failed/unknown evidence cases |
| Privacy boundary regression | required | required | optional | required | optional | required | not required | representative benign/sensitive fixtures and negative leakage tests |
| Release readiness report | required | optional | optional | required | required | required | optional | gate matrix, checklist, open-risk list, public-safe artifact links |

## Local-First Minimums

A local slice is acceptable when:

- the focused test for the touched behavior passes
- the strongest relevant module gate passes, usually `spotlessCheck :app:allTests :app:build` or the touched module equivalent
- `bash scripts/validate-public-hygiene.sh` passes
- `git diff --check` passes
- the PR body records exact commands and any skipped/manual evidence

Release-ready requires green PR CI. Manual QA may supplement but does not
replace required tests, hygiene, coverage, or platform smoke checks.

## Privacy-Sensitive Paths

Any path that handles runtime data, persisted records, diagnostics, logs,
screenshots, reports, issue comments, PR bodies, or UI summaries requires
negative leakage tests. At minimum, tests should cover synthetic:

- tokens and credentials
- private URLs, private hosts, file URLs, and URLs with userinfo
- raw transcript markers
- local Linux, macOS, Windows, and shell-home paths
- raw channel/message ids and `channel:`, `message:`, `thread:`, or `session:` markers
- private runtime/session ids

Failures must not echo the private sample payload. Diagnostics should say which
public-safe boundary rejected the input and what the operator can inspect next.

## Manual And Optional Checks

Manual/local operator validation is useful for:

- checking UI layout, screenshots, focus behavior, or accessibility of changed UI
- inspecting generated reports for public-safe wording
- verifying local smoke commands that are too slow or environment-specific for every PR
- recording known residual risks in verification evidence

Manual checks are optional unless the capability matrix marks them required.
When skipped, the PR should say why and whether the omission creates residual
risk.

## Non-Goals

This matrix does not define comprehensive compliance controls, secret scanning,
full DLP, private alias discovery, production release sign-off, or provider
permission APIs. It is the roadmap acceptance gate for the current local-first
Agent Desk product surface.
