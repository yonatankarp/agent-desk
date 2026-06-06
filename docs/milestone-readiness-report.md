# Milestone Readiness Report And Release Checklist

Use this artifact to decide whether Agent Desk milestone work is actually ready.
An empty queue, quiet discovery pass, or merged PR list is not completion by
itself. Readiness requires fresh verification evidence mapped to acceptance
criteria, privacy/security gates, and known gaps.

Related references:

- [Roadmap verification gate matrix](roadmap-verification-gate-matrix.md)
- [Verification evidence](verification-evidence.md)
- [Local-first smoke suite](local-first-smoke-suite.md)
- [Public-safe artifact policy](public-safe-artifact-policy.md)
- [Milestone completion criteria](milestone-completion-criteria.md)

## Report Template

```text
Milestone:
Date:
Owner:
Recommended status: ready | not-ready | blocked | unknown

Completed work:
- Issue/PR/commit:
- Capability:
- Evidence:

Open gaps:
- Gap:
- Impact:
- Required next action:

Risks:
- Risk:
- Likelihood/impact:
- Mitigation:

Verification evidence:
- Local tests:
- Integration tests:
- Smoke tests:
- Coverage:
- Manual QA:
- CI status:

Privacy/security status:
- Public hygiene:
- Negative leakage tests:
- Permission/external-side-effect review:
- Artifact policy review:
- Status: ready | not-ready | blocked | unknown

Release checklist:
- Tests:
- Coverage:
- Smokes:
- Privacy checks:
- Security checks:
- Docs:
- Rollback/disablement notes:
- Known gaps:

Recommended next actions:
- Ship:
- Hold:
- Follow-up:
```

## Minimum Ready Criteria

A milestone may be marked ready only when all of these are true:

- required capability gates in the verification matrix are green or explicitly waived
- PR CI is green for the release candidate commit
- `bash scripts/validate-public-hygiene.sh` passes
- `make smoke-local-first` passes in a Java-ready shell
- coverage verification passes
- privacy/security status is known and ready
- release notes or readiness notes list known gaps and residual risks
- rollback or disablement notes exist for any risky operator path

Privacy/security gate status cannot be `unknown` for a ready release. If public
hygiene, negative leakage tests, permission gates, or artifact review are
missing, the milestone is `not-ready` or `blocked`.

## Local Verification Commands

Run these from the repository root:

```bash
bash scripts/validate-public-hygiene.sh
git diff --check
./gradlew spotlessCheck
./gradlew :core:build :app:build :cli:build :desktop:build :mobile:build
./gradlew :core:allTests :app:allTests :cli:test :desktop:allTests :mobile:allTests
make smoke-local-first
```

If the shell cannot find Java, set `JAVA_HOME` or put Java on `PATH` before
running Gradle or Makefile smoke targets. Missing Java is local setup failure;
failing assertions inside the smoke output are product regressions.

## Checklist

### Tests

- Focused tests for touched behavior passed.
- Module-level or repo-level tests passed.
- Failure messages are actionable and public-safe.
- Skipped tests are listed with reason and residual risk.

### Coverage

- CI coverage job passed.
- Coverage comment or artifact is linked.
- Any coverage decline is explained or fixed.

### Smokes

- `make smoke-local-first` passed.
- UI smoke/manual review is included for UI changes.
- Smoke output uses only public-safe fixtures and temporary local state.

### Privacy Checks

- Public hygiene passed.
- Negative leakage tests cover privacy-sensitive paths touched by the milestone.
- Reports, screenshots, diagnostics, and issue/PR text follow the artifact policy.

### Security Checks

- Permission gates are tested for action-taking paths.
- External sends, public posts, destructive actions, account/security changes,
  purchases/payments, and credential actions are unavailable or explicitly gated.
- Accidental external action runbook is linked when relevant.

### Docs

- New commands, gates, and runbooks are linked from `docs/index.md`.
- Operator-facing docs include how to run and interpret checks.
- Known gaps and non-goals are explicit.

### Rollback Or Disablement

- Risky paths have a disablement or no-executor note.
- Release can be held without losing local replay/inspection ability.
- Follow-up issues exist for gaps that block readiness.

## Current-State Use

For the current repo, this report can be filled manually from:

- merged issue and PR history
- latest PR CI status
- local command output
- verification gate matrix
- privacy boundary regression results
- local-first smoke output

Do not paste private logs, raw runtime context, screenshots with private content,
or raw tool output into the report. Store only public-safe summaries, issue/PR
links, commit ids, check names, and sanitized evidence references.
