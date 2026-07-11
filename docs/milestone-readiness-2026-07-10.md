# Milestone Readiness Report - 2026-07-10

Milestone: first local-first supervisor loop, plus accepted live-host follow-up stages through approval-gated inspect
Date: 2026-07-10
Owner: repository owner
Recommended status: ready

This report supersedes the 2026-07-09 readiness snapshot for the current
release-candidate check. It maps the current `main` state at `7aec1f6` to the
release checklist in [Milestone readiness report and release checklist](milestone-readiness-report.md).
It is public-safe by design: evidence is limited to public issue, pull request,
commit, workflow, command, and documentation references.

## Completed Work Reviewed

- Issue/PR/commit: [#423](https://github.com/yonatankarp/agent-desk/issues/423)
  and `7aec1f6`
  Capability: refreshed manual desktop and mobile UI evidence for the current
  release candidate.
  Evidence: [Manual Desktop And Mobile UI Evidence - 2026-07-10](manual-ui-evidence-2026-07-10.md)
  records the dated public-safe review of the desktop and mobile display gates.
- Issue/PR/commit: [#421](https://github.com/yonatankarp/agent-desk/issues/421),
  [#422](https://github.com/yonatankarp/agent-desk/pull/422), and `7aec1f6`
  Capability: current milestone readiness report with automated gates green and
  the manual UI gap called out.
  Evidence: [Milestone Readiness Report - 2026-07-09](milestone-readiness-2026-07-09.md)
  records the previous not-ready state and points to the exact evidence gap
  closed by this report.
- Issue/PR/commit: [#417](https://github.com/yonatankarp/agent-desk/issues/417),
  [#418](https://github.com/yonatankarp/agent-desk/pull/418), and `080577f`
  Capability: mismatch behavior for live inspect approval smoke.
  Evidence: recent public commit history and successful `main` CI after merge.
- Issue/PR/commit: [#415](https://github.com/yonatankarp/agent-desk/issues/415),
  [#416](https://github.com/yonatankarp/agent-desk/pull/416), and `47144f1`
  Capability: live inspect smoke command.
  Evidence: `scripts/live-inspect-smoke.sh` exists and `make smoke` includes
  `smoke-live-inspect`.
- Issue/PR/commit: [#387](https://github.com/yonatankarp/agent-desk/issues/387),
  [#414](https://github.com/yonatankarp/agent-desk/pull/414), and `757ad29`
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
- Risk: manual UI evidence can drift after successful CI.
  Likelihood/impact: medium likelihood, medium release impact.
  Mitigation: this report pins the readiness conclusion to dated command
  evidence and a dated public-safe manual UI evidence artifact.
- Risk: the manual UI pass uses deterministic public-safe sample and
  stored-event display paths rather than private operator runtime state.
  Likelihood/impact: medium likelihood, low release-candidate impact.
  Mitigation: this is intentional for a public repository; private runtime
  screenshots and raw provider payloads remain outside tracked artifacts.

## Verification Evidence

- Local tests: `bash scripts/validate-public-hygiene.sh`, `git diff --check`,
  `./gradlew :desktop:allTests`, and `./gradlew :mobile:allTests` passed for the
  refreshed UI evidence pass.
- Integration tests: desktop and mobile module all-tests passed; the 2026-07-09
  full module build/test gate remains the current full release-candidate
  integration evidence for `7aec1f6`.
- Smoke tests: `bash scripts/compose-run-smoke.sh`,
  `bash scripts/mobile-read-only-smoke.sh`, and `make smoke-local-first` passed.
  During `make smoke-local-first`, the mock action denial path printed the
  expected non-zero CLI subprocess summary before the smoke script confirmed the
  denied action and completed successfully.
- Coverage: `make coverage` passed and generated Kover XML/HTML reports for
  `:core`, `:app`, `:cli`, `:desktop`, and `:mobile`.
- Manual QA: refreshed in [Manual Desktop And Mobile UI Evidence - 2026-07-10](manual-ui-evidence-2026-07-10.md);
  no screenshot file was committed because the current public-safe evidence path
  is the deterministic smoke snapshot plus non-interactive Compose run smoke.
- CI status: the latest public `main` CI workflow for `7aec1f6` completed
  successfully on 2026-07-09.

## Privacy And Security Status

- Public hygiene: `bash scripts/validate-public-hygiene.sh` passed.
- Negative leakage tests: rerun through the coverage and focused desktop/mobile
  test gates.
- Permission/external-side-effect review: no live runtime action was executed by
  this report. The referenced live inspect flow remains approval-gated and
  read-only by accepted scope.
- Artifact policy review: this report and the manual UI evidence artifact use
  public issues, PRs, commit ids, workflow status summaries, command names, and
  relative docs paths only.
- Status: ready.

## Release Checklist

- Tests: ready; focused desktop and mobile all-tests passed, and the previous
  full release-candidate module gate remains green for `7aec1f6`.
- Coverage: ready; `make coverage` passed.
- Smokes: ready; local-first, Compose run, and mobile read-only smoke checks
  passed.
- Privacy checks: ready; public hygiene and Gradle-backed negative leakage tests
  passed.
- Security checks: ready; permission-gate and no-external-side-effect behavior
  is covered through tests and smoke gates.
- Docs: ready; this report and the manual UI evidence artifact are linked from
  the docs index.
- Rollback/disablement notes: available for live inspect through host permission
  mode downgrade in [Live host action approval](live-host-action-approval-proposal.md).
- Known gaps: #279 remains blocked on upstream API stabilization.

## Recommended Next Actions

- Ship: the current milestone can be treated as ready for the first local-first
  supervisor loop release candidate.
- Hold: hold only if owner review requires a stored screenshot artifact instead
  of the current deterministic public-safe smoke snapshot evidence path.
- Follow-up: re-check #279 on future Compose Multiplatform version bumps.
