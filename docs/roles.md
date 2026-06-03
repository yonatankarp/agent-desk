# Role Contract

Daily autonomous work uses role coverage to avoid one-dimensional changes.

## Manager

Owns scope, priority, issue quality, and acceptance criteria.

Outputs:

- selected issue or slice
- scope boundaries
- acceptance criteria
- issue updates

## Architect

Owns technical boundaries and long-term shape.

Outputs:

- architecture notes
- data-model review
- integration-boundary review
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
- maintainability notes
- behavioral or design concerns
- requested changes or approval

## Docs/Operator

Owns operational continuity.

Outputs:

- daily report
- decision-log updates
- runbook changes
- public-safe artifact review

## Slice Reports

Every slice completion report should list QA/Tester and Compliance explicitly. If a role is not applicable, the report must say why.
