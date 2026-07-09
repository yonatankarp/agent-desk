# Milestone Readiness Report - 2026-07-09

Milestone: first local-first supervisor loop, plus accepted live-host follow-up stages through approval-gated inspect
Date: 2026-07-09
Owner: repository owner
Recommended status: not-ready

This report supersedes the 2026-07-08 readiness snapshot for the current
release-candidate check. It maps the current `main` state at `b466ca4` to the
release checklist in [Milestone readiness report and release checklist](milestone-readiness-report.md).
It is public-safe by design: evidence is limited to public issue, pull request,
commit, workflow, command, and documentation references.

## Completed Work Reviewed

- Issue/PR/commit: [#419](https://github.com/yonatankarp/agent-desk/issues/419),
  [#420](https://github.com/yonatankarp/agent-desk/pull/420), and
  `b466ca4`
  Capability: current milestone readiness report.
  Evidence: [Milestone Readiness Report - 2026-07-08](milestone-readiness-2026-07-08.md)
  records completed live inspect evidence, open gaps, and required release
  checklist follow-up.
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

- Gap: current manual desktop and mobile UI evidence was not refreshed or waived.
  Impact: the verification matrix marks desktop and mobile display surfaces as
  requiring manual QA when evaluating release readiness. Automated desktop,
  Compose, and mobile smoke checks passed, but this non-interactive daily run
  only exercised automated smoke paths and did not capture a manual screenshot
  or interactive review artifact.
  Required next action: capture public-safe desktop and mobile UI evidence in a
  dedicated manual QA pass, or explicitly document an owner-approved waiver for
  this release candidate.
- Gap: issue [#279](https://github.com/yonatankarp/agent-desk/issues/279)
  remains open and blocked on upstream Compose test API stabilization.
  Impact: the repo still carries the accepted Compose UI test experimental API
  exception.
  Required next action: re-check #279 on Compose Multiplatform version bumps.

## Risks

- Risk: green hosted CI and green automated local checks may be mistaken for
  full release readiness.
  Likelihood/impact: medium likelihood, high release-reporting impact.
  Mitigation: this report keeps status `not-ready` until manual/UI evidence is
  refreshed or explicitly waived.
- Risk: live inspect is read-only but can reach a configured host when enabled.
  Likelihood/impact: medium likelihood, medium safety impact.
  Mitigation: the accepted action proposal keeps inspect behind one-shot
  approval, public-safe aliases, fail-closed permission checks, and audit
  evidence before adapter results are rendered.
- Risk: manual UI and release-candidate evidence can drift after successful CI.
  Likelihood/impact: medium likelihood, medium release impact.
  Mitigation: require a dated release checklist and fresh command output before
  changing the status to `ready`.

## Verification Evidence

- Local tests: `bash scripts/validate-public-hygiene.sh`, `git diff --check`,
  and `./gradlew :core:build :test-fixtures:build :app:build :cli:build :desktop:build :mobile:build :core:allTests :test-fixtures:allTests :app:allTests :cli:test :desktop:allTests :mobile:allTests`
  passed.
- Integration tests: the full Gradle module build/test gate above passed.
- Smoke tests: `make smoke-local-first`, `bash scripts/compose-run-smoke.sh`,
  and `bash scripts/mobile-read-only-smoke.sh` passed.
- Coverage: `make coverage` passed and generated Kover XML/HTML reports for
  `:core`, `:app`, `:cli`, `:desktop`, and `:mobile`.
- Manual QA: not refreshed in this pass; no public-safe manual screenshot or
  interactive review artifact was captured.
- CI status: the latest public `main` workflow for `b466ca4` completed
  successfully on 2026-07-08.

## Privacy And Security Status

- Public hygiene: `bash scripts/validate-public-hygiene.sh` passed for this
  report slice.
- Negative leakage tests: rerun through the Gradle test and coverage gates.
- Permission/external-side-effect review: no live runtime action was executed by
  this report. The referenced live inspect flow remains approval-gated and
  read-only by accepted scope.
- Artifact policy review: this report uses public issues, PRs, commit ids,
  workflow status summaries, command names, and relative docs paths only.
- Status: not-ready until manual/UI evidence is refreshed or explicitly waived.

## Release Checklist

- Tests: ready; full local Gradle module build/test gate passed.
- Coverage: ready; `make coverage` passed.
- Smokes: ready for automated evidence; local-first, Compose run, and mobile
  read-only smoke checks passed.
- Privacy checks: ready for automated evidence; public hygiene and
  Gradle-backed negative leakage tests passed.
- Security checks: ready for automated evidence; permission-gate and
  no-external-side-effect checks passed through the local test and smoke gates.
- Docs: updated with this not-ready readiness report and linked from the docs
  index.
- Rollback/disablement notes: available for live inspect through host permission
  mode downgrade in [Live host action approval](live-host-action-approval-proposal.md).
- Known gaps: manual UI evidence was not refreshed or waived, and #279 remains
  blocked on upstream API stabilization.

## Recommended Next Actions

- Ship: do not mark the milestone ready from this report.
- Hold: keep status `not-ready` until public-safe manual desktop and mobile UI
  evidence is refreshed or explicitly waived.
- Follow-up: run a dedicated manual UI evidence pass, then update or supersede
  this report before declaring the milestone ready.
