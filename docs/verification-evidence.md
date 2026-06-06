# Verification Evidence

Verification evidence is structured proof that an Agent Desk slice or milestone
claim was checked. A merged pull request, empty queue, or stopped agent is not
completion by itself; completion requires fresh evidence mapped to the relevant
criteria.

## Verification Result

Each result records:

- command or check name
- kind: local test, CI check, smoke run, or manual QA
- result: passed, failed, skipped, or unknown
- duration when known
- output reference such as `artifact:verification-output`
- failure summary for failed checks
- freshness: fresh, stale, or unknown
- compact public-safe evidence reference

Public-safe example:

```text
name: app verification tests
kind: local-test
result: passed
duration: 1200ms
output: artifact:verification-output
freshness: fresh
evidence: sanitized-note Verification evidence -> artifact:verification-output
```

Failed checks must include a compact failure summary:

```text
name: Windows Compose Build
kind: ci-check
result: failed
output: github-actions:windows-compose-build
failure: Gradle task failed.
freshness: fresh
evidence: sanitized-note Verification evidence -> github-actions:windows-compose-build
```

## Completion Checklist

Completion evidence records:

- outcome: ready, not ready, blocked, or unknown
- whether verification was attempted
- known failures
- touched artifacts
- residual risks
- the structured verification results used by the claim

Readiness is derived conservatively. Skipped, failed, stale, unknown, missing,
or unattempted verification makes the checklist not ready unless the whole
outcome is blocked or unknown. Manual QA may supplement automated checks, but it
does not replace required tests, smoke runs, public hygiene, or CI evidence.

Non-goals for this slice: CI provider integration, private logs, screenshots
with private data, raw transcripts, retention policy, cloud storage, and
compliance-grade audit controls.
