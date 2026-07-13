# Milestone Readiness Report - 2026-07-13

Milestone: first local-first supervisor loop, plus accepted live-host follow-up
stages through approval-gated inspect
Date: 2026-07-13
Owner: repository owner
Recommended status: ready

This report supersedes the 2026-07-10 readiness snapshot for the current
release-candidate check. It maps the current `main` state at `08a0e8c` to the
release checklist in [Milestone readiness report and release checklist](milestone-readiness-report.md).
It is public-safe by design: evidence is limited to public issue, pull request,
commit, workflow, command, and documentation references.

## Completed Work Reviewed

- Issue/PR/commit: [#431](https://github.com/yonatankarp/agent-desk/issues/431)
  and this report.
  Capability: refreshed readiness evidence after recent Settings and dependency
  updates.
  Evidence: this document records the dated public-safe review for current
  `main`.
- Issue/PR/commit: [#425](https://github.com/yonatankarp/agent-desk/issues/425),
  [#426](https://github.com/yonatankarp/agent-desk/pull/426), and `47a5d31`.
  Capability: shared Settings surface across desktop and mobile.
  Evidence: `OperatorDisplaySection`, `SettingsDisplay`, desktop and mobile
  Settings rendering, and their smoke tests were added or updated in the merged
  slice. Follow-up issue
  [#427](https://github.com/yonatankarp/agent-desk/issues/427) was closed after
  inspection found the current `main` state already had tab-like navigation
  semantics and shared Settings labels.
- Issue/PR/commit: [#428](https://github.com/yonatankarp/agent-desk/pull/428),
  `d5b6f39`.
  Capability: Kotest dependency update from 6.2.1 to 6.2.2.
  Evidence: latest `main` CI completed successfully after the dependency update.
- Issue/PR/commit: [#430](https://github.com/yonatankarp/agent-desk/pull/430),
  `e7ad5b7`.
  Capability: Shadow dependency update from 9.5.0 to 9.5.1.
  Evidence: latest `main` CI completed successfully after the dependency update.
- Issue/PR/commit: [#429](https://github.com/yonatankarp/agent-desk/pull/429),
  `08a0e8c`.
  Capability: KSP dependency update from 2.3.9 to 2.3.10.
  Evidence: latest `main` CI completed successfully at `08a0e8c`.
- Issue/PR/commit: [#423](https://github.com/yonatankarp/agent-desk/issues/423),
  [#424](https://github.com/yonatankarp/agent-desk/pull/424), and `7b54262`.
  Capability: refreshed manual desktop and mobile UI evidence for the previous
  release-candidate check.
  Evidence: [Manual Desktop And Mobile UI Evidence - 2026-07-10](manual-ui-evidence-2026-07-10.md)
  remains the latest dedicated manual UI evidence artifact, and current CI still
  exercises the desktop and mobile smoke/build gates after the Settings update.
- Issue/PR/commit: [#417](https://github.com/yonatankarp/agent-desk/issues/417),
  [#418](https://github.com/yonatankarp/agent-desk/pull/418), and `080577f`.
  Capability: mismatch behavior for live inspect approval smoke.
  Evidence: recent public commit history and successful `main` CI after merge.
- Issue/PR/commit: [#415](https://github.com/yonatankarp/agent-desk/issues/415),
  [#416](https://github.com/yonatankarp/agent-desk/pull/416), and `47144f1`.
  Capability: live inspect smoke command.
  Evidence: `scripts/live-inspect-smoke.sh` exists and `make smoke` includes
  `smoke-live-inspect`.
- Issue/PR/commit: [#387](https://github.com/yonatankarp/agent-desk/issues/387),
  [#414](https://github.com/yonatankarp/agent-desk/pull/414), and `757ad29`.
  Capability: approval-gated live host inspect path.
  Evidence: [Live host action approval](live-host-action-approval-proposal.md)
  records the accepted read-only inspect scope, public-safe proposal shape,
  audit evidence, and explicit non-goals for mutating live actions.

## Open Gaps

- Gap: issue [#279](https://github.com/yonatankarp/agent-desk/issues/279)
  remains open and blocked on upstream Compose test API stabilization.
  Impact: the repo still carries the accepted Compose UI test experimental API
  exception.
  Required next action: re-check #279 on Compose Multiplatform version bumps.

## Risks

- Risk: green hosted CI and green automated local checks may be mistaken for
  permission to run live mutating actions.
  Likelihood/impact: medium likelihood, high safety impact.
  Mitigation: live inspect remains read-only and approval-gated; mutating live
  actions remain out of scope until a separate owner-approved decision.
- Risk: manual UI evidence can drift after Settings UI changes.
  Likelihood/impact: medium likelihood, medium release impact.
  Mitigation: the shared Settings slice added desktop and mobile smoke coverage,
  and the latest `main` CI exercises desktop and mobile build/smoke gates. Hold
  release only if owner review requires a new dedicated manual UI artifact after
  the Settings surface change.
- Risk: the manual UI pass uses deterministic public-safe sample and
  stored-event display paths rather than private operator runtime state.
  Likelihood/impact: medium likelihood, low release-candidate impact.
  Mitigation: this is intentional for a public repository; private runtime
  screenshots and raw provider payloads remain outside tracked artifacts.

## Verification Evidence

- Local tests: `bash scripts/validate-public-hygiene.sh`, `git diff --check`,
  and pre-push `./gradlew spotlessCheck` passed while preparing this docs-only
  readiness refresh.
- Integration tests: latest `main` CI at `08a0e8c` passed Gradle module builds
  for core, test-fixtures, app, CLI, design, desktop, and mobile.
- Smoke tests: latest `main` CI at `08a0e8c` passed mock runtime smoke, mobile
  read-only smoke, and Compose run task smoke.
- Coverage: latest `main` CI at `08a0e8c` passed coverage threshold verification
  and generated module coverage reports.
- Manual QA: [Manual Desktop And Mobile UI Evidence - 2026-07-10](manual-ui-evidence-2026-07-10.md)
  remains the latest manual UI artifact; no new screenshot file was committed
  because the current public-safe evidence path is deterministic smoke coverage
  plus non-interactive Compose run smoke.
- CI status: the latest public `main` CI workflow for `08a0e8c` completed
  successfully on 2026-07-13.

## Privacy And Security Status

- Public hygiene: `bash scripts/validate-public-hygiene.sh` passed.
- Negative leakage tests: latest `main` CI ran the public hygiene self-test and
  coverage-backed test gates.
- Permission/external-side-effect review: no live runtime action was executed by
  this report. The referenced live inspect flow remains approval-gated and
  read-only by accepted scope.
- Artifact policy review: this report uses public issues, PRs, commit ids,
  workflow status summaries, command names, and relative docs paths only.
- Status: ready.

## Release Checklist

- Tests: ready; current `main` CI passed Gradle module builds after the Settings
  and dependency update merges.
- Coverage: ready; current `main` CI passed coverage threshold verification.
- Smokes: ready; current `main` CI passed mock runtime, mobile read-only, and
  Compose run task smoke checks.
- Privacy checks: ready; public hygiene and the public hygiene self-test passed.
- Security checks: ready; permission-gate and no-external-side-effect behavior
  remains covered through tests, smoke gates, and the accepted live inspect
  boundary.
- Docs: ready; this report is linked from the docs index.
- Rollback/disablement notes: available for live inspect through host permission
  mode downgrade in [Live host action approval](live-host-action-approval-proposal.md).
- Known gaps: #279 remains blocked on upstream API stabilization.

## Recommended Next Actions

- Ship: the current milestone can be treated as ready for the first local-first
  supervisor loop release candidate at `08a0e8c`.
- Hold: hold only if owner review requires a fresh dedicated manual UI evidence
  artifact after the shared Settings surface.
- Follow-up: re-check #279 on future Compose Multiplatform version bumps.
