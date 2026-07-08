# Milestone Readiness Report - 2026-07-08

Milestone: first local-first supervisor loop, plus accepted live-host follow-up stages through approval-gated inspect
Date: 2026-07-08
Owner: repository owner
Recommended status: not-ready

This report maps the current `main` state to the readiness template in
[Milestone readiness report and release checklist](milestone-readiness-report.md).
It is public-safe by design: evidence is limited to public issue, pull request,
commit, workflow, command, and documentation references.

## Completed Work

- Issue/PR/commit: [#389](https://github.com/yonatankarp/agent-desk/issues/389),
  [#408](https://github.com/yonatankarp/agent-desk/pull/408), and
  `b14399c`
  Capability: accepted approval flow for the first live-host inspect action.
  Evidence: [Live host action approval](live-host-action-approval-proposal.md)
  records the accepted scope, public-safe proposal shape, audit evidence, and
  explicit non-goals for mutating live actions.
- Issue/PR/commit: [#387](https://github.com/yonatankarp/agent-desk/issues/387),
  [#414](https://github.com/yonatankarp/agent-desk/pull/414), and
  `757ad29`
  Capability: approval-gated live host inspect path.
  Evidence: public commit history and CI on `main`; the approval proposal keeps
  inspect behind an explicit approval and audit boundary.
- Issue/PR/commit: [#415](https://github.com/yonatankarp/agent-desk/issues/415),
  [#416](https://github.com/yonatankarp/agent-desk/pull/416), and
  `47144f1`
  Capability: live inspect smoke command.
  Evidence: `scripts/live-inspect-smoke.sh` exists and `make smoke` includes
  `smoke-live-inspect`.
- Issue/PR/commit: [#417](https://github.com/yonatankarp/agent-desk/issues/417),
  [#418](https://github.com/yonatankarp/agent-desk/pull/418), and
  `080577f`
  Capability: mismatch behavior for live inspect approval smoke.
  Evidence: latest `main` CI completed successfully on 2026-07-07 for commit
  `080577f`.
- Issue/PR/commit: [#390](https://github.com/yonatankarp/agent-desk/issues/390),
  [#405](https://github.com/yonatankarp/agent-desk/pull/405), and
  [#382](https://github.com/yonatankarp/agent-desk/issues/382)
  Capability: read-only live observation sync and host connectivity status.
  Evidence: [Live host connectivity milestone](live-host-connectivity-milestone.md)
  lists the staged sequence and public-safe evidence constraints.

## Open Gaps

- Gap: no release candidate has been explicitly declared ready after the latest
  live inspect slices.
  Impact: green `main` CI proves recent integration, but it does not by itself
  establish milestone readiness.
  Required next action: run the release checklist on the intended release
  candidate and update this report or create a successor report with the final
  evidence map.
- Gap: current manual UI evidence is not refreshed in this report.
  Impact: the verification matrix marks desktop and mobile display surfaces as
  requiring manual QA when evaluating release readiness.
  Required next action: collect public-safe desktop and mobile UI evidence, or
  explicitly document why the release candidate does not require new UI manual
  QA.
- Gap: issue [#279](https://github.com/yonatankarp/agent-desk/issues/279)
  remains open and blocked on upstream Compose test API stabilization.
  Impact: the repo still carries the accepted Compose UI test experimental API
  exception.
  Required next action: re-check #279 on Compose Multiplatform version bumps.

## Risks

- Risk: empty actionable issue queues may be mistaken for product completion.
  Likelihood/impact: medium likelihood, high product-reporting impact.
  Mitigation: this report states `not-ready` until readiness gates are fresh and
  mapped.
- Risk: live inspect is read-only but still reaches a configured host when
  enabled.
  Likelihood/impact: medium likelihood, medium safety impact.
  Mitigation: the accepted action proposal requires one-shot approval, public-safe
  aliases, fail-closed permission checks, and audit evidence before any adapter
  result is rendered.
- Risk: manual UI and release-candidate evidence can drift after successful CI.
  Likelihood/impact: medium likelihood, medium release impact.
  Mitigation: require a dated release checklist and fresh command output before
  changing the status to `ready`.

## Verification Evidence

- Local tests: `bash scripts/validate-public-hygiene.sh`, `git diff --check`,
  `./gradlew spotlessCheck`, and `make smoke-local-first` passed for this report
  slice on 2026-07-08. Gradle-backed commands required setting `JAVA_HOME` in
  the cron shell because Java was installed but not on `PATH`.
- Integration tests: latest `main` CI completed successfully for commit
  `080577f` on 2026-07-07.
- Smoke tests: `make smoke-local-first` passed. `scripts/live-inspect-smoke.sh`
  is included in `make smoke` and remains the broader live inspect smoke target.
- Coverage: latest `main` CI completed successfully for commit `080577f`;
  release readiness still requires checking the coverage gate for the intended
  release candidate.
- Manual QA: not refreshed in this report.
- CI status: latest `main` CI completed successfully for commit `080577f`.

## Privacy And Security Status

- Public hygiene: `bash scripts/validate-public-hygiene.sh` passed for this
  docs slice.
- Negative leakage tests: no runtime, persistence, diagnostics, or UI source
  changed in this report; existing negative leakage coverage is unchanged.
- Permission/external-side-effect review: no external runtime action is executed
  by this report. The referenced live inspect flow remains approval-gated and
  read-only by accepted scope.
- Artifact policy review: this report uses public issues, PRs, commit ids,
  workflow status summaries, command names, and relative docs paths only.
- Status: not-ready until the release checklist is freshly run and manual/UI
  evidence is either captured or explicitly waived for the release candidate.

## Release Checklist

- Tests: not ready; run the release candidate test set from
  [Milestone readiness report and release checklist](milestone-readiness-report.md).
- Coverage: not ready; verify the coverage gate for the release candidate.
- Smokes: not ready; run `make smoke-local-first`, and run broader smoke targets
  when the release candidate includes desktop, mobile, CLI, or live-host action
  surfaces.
- Privacy checks: public hygiene passed for this report slice.
- Security checks: conditionally ready for documentation only; product readiness
  still requires confirming permission gates and no-external-side-effect behavior
  on the release candidate.
- Docs: ready for current evidence mapping; this report is linked and locally
  verified.
- Rollback/disablement notes: available for live inspect through host permission
  mode downgrade in
  [Live host action approval](live-host-action-approval-proposal.md).
- Known gaps: no declared release candidate, no refreshed manual UI evidence,
  and #279 remains blocked on upstream API stabilization.

## Recommended Next Actions

- Ship: do not mark the milestone ready from this report alone.
- Hold: keep status `not-ready` until release-candidate gates are fresh.
- Follow-up: use this report as the starting checklist for the next readiness
  pass, then update or supersede it with concrete command results, CI links, and
  manual UI evidence.
