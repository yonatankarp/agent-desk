# Verification Evidence

Verification evidence is structured proof that an Agent Desk slice or milestone
claim was checked. A merged pull request, empty queue, or stopped agent is not
completion by itself; completion requires fresh evidence mapped to the relevant
criteria.

## Verification Result

Each result records:

- name of the check
- kind: local test, CI check, smoke run, or manual QA
- result: passed, failed, skipped, or unknown
- duration when known
- output reference such as `artifact:verification-output`
- failure summary for failed checks
- optional input binding: the SHA-256 content digest of the artifact that was
  checked, plus the algorithm and the instant the digest was captured
- compact public-safe evidence reference

Freshness is not recorded on the result; it is derived (see below). The content
digest detects *drift* — whether the checked artifact changed after it was
verified — not forgery: results are built in-process and nothing signs them. The
digest is a fixed lowercase-hex string over a public-safe artifact, so it cannot
carry a path, secret, or private id.

Public-safe example:

```text
name: app verification tests
kind: local-test
result: passed
duration: 1200ms
output: artifact:verification-output
input-binding: sha256 9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08 @ 2026-06-02T21:20:00Z
evidence: sanitized-note Verification evidence -> artifact:verification-output
```

Failed checks must include a compact failure summary:

```text
name: Windows Compose Build
kind: ci-check
result: failed
output: github-actions:windows-compose-build
failure: Gradle task failed.
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

Readiness is derived conservatively. Freshness is derived per result by
comparing its input binding's captured instant against the work item's last
change: evidence verified at or after the last change is fresh, evidence
verified before it is stale, and evidence with no binding (or no reference
point) is unknown — unbound evidence can never count as fresh. Skipped, failed,
stale, unknown, missing, or unattempted verification makes the checklist not
ready unless the whole outcome is blocked or unknown. Manual QA may supplement
automated checks, but it does not replace required tests, smoke runs, public
hygiene, or CI evidence.

Non-goals for this slice: CI provider integration, private logs, screenshots
with private data, raw transcripts, retention policy, cloud storage, and
compliance-grade audit controls.
