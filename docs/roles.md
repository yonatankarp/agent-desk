# Role Contract

Daily autonomous work uses role coverage to avoid one-dimensional changes.

## Manager

Owns scope, priority, issue quality, acceptance criteria, and Product-style backlog shaping when the issue list is empty or exhausted.

Outputs:

- selected issue or slice
- backlog triage, including why closed issues, blocked issues, stale worktrees, or decision issues were skipped or selected
- no-issue triage from `VISION.md`, recent commits, CI state, docs drift, roadmap gaps, and operator pain when no open issues exist
- newly created or proposed issues, with Goal, Acceptance Criteria, Verification, and Notes
- scope boundaries
- acceptance criteria
- issue updates

## Architect

Owns technical boundaries and long-term shape.

Outputs:

- architecture notes
- data-model review
- integration-boundary review
- dependency-direction and package/module placement review
- migration or coupling risks

## Developer

Owns implementation.

Outputs:

- code changes
- local verification
- implementation notes

## QA/Tester

Owns verification evidence, local checks, CI status, regression risk, and explicit test gaps.

Outputs:

- checks run and their result
- CI status
- regression risk notes
- anything not run, with reason

## Compliance

Owns whether the slice satisfies its stated obligations.

Outputs:

- acceptance criteria review
- engineering-style and architecture-rule review
- public-safety and user-constraint review
- documented gaps or follow-up issues

Compliance is separate from QA, Security, and Reviewer. QA proves behavior through checks; Security focuses on secrets, privacy, permissions, adapters, persistence, and public-safe boundaries; Reviewer focuses on code, design, or implementation quality.

Every Compliance review for a code slice must include an architecture boundary check covering dependency direction, package/module placement, forbidden imports, and issue acceptance criteria. If no boundary is relevant, say why.

## Security

Owns secrets, privacy, permissions, adapters, persistence, and public-safe boundaries.

Outputs:

- threat, secret, permission, and public-repo exposure review
- adapter and persistence risk notes
- security follow-up issues when needed

## Designer

Owns visible UX and interaction quality.

Required when UI, product copy, navigation, information architecture, or visual state changes.

Outputs:

- UX review
- layout or interaction notes
- screenshots when available

## Reviewer

Owns code, design, or implementation review separate from QA evidence.

Outputs:

- diff risks
- dependency, package, and boundary concerns missed by implementation
- maintainability notes
- behavioral or design concerns
- requested changes or approval

## Discovery

Owns follow-up discovery after merged or reviewed slices.

Outputs:

- post-merge acceptance-criteria audit
- CodeRabbit/review findings, or an explicit note that they were unavailable, skipped, or rate-limited
- codebase-scan findings from package/module boundaries, TODO/FIXME notes, recently touched areas, public APIs, docs drift, and thin or skipped tests
- follow-up issues with Goal, Acceptance Criteria, and Verification when real gaps exist
- explicit "no follow-up issues warranted" note when the audit finds no implementable follow-up

Discovery should not create noisy issues for every idea. It should favor small, implementable issues tied to evidence from the slice, codebase scan, docs, tests, or review comments.

Discovery must not revive closed issues or stale local workspaces as new work unless the owner explicitly asks for a rerun. When the open backlog contains only decision issues, Discovery should report that no independent follow-up issue was warranted yet and hand control back to Manager to resolve the decision issue next.

In a timeboxed run, Discovery is the handoff back to Manager, not the end of the run by default. If it creates or identifies a new unblocked slice and meaningful time remains, the orchestrator should return to Manager selection.

## Docs/Operator

Owns operational continuity.

Outputs:

- daily report
- decision-log updates
- runbook changes
- public-safe artifact review

## Slice Reports

Every slice completion report should list QA/Tester and Compliance explicitly. If a role is not applicable, the report must say why.

Compliance entries in slice reports should include `Architecture boundary check:` with the result or a clear not-applicable reason.

Every loop report should include Discovery output. The report must list new follow-up issues or state why no follow-up issues were warranted.
