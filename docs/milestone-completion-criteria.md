# Agent Desk Milestone Completion Criteria

Agent Desk is complete for a milestone only when the product workflow is
observable, verified, and public-safe. An empty GitHub issue queue, a merged
PR, or a successful import is useful evidence, but none of those signals means
the product is complete on its own.

This document defines first-milestone product completion. `VISION.md` remains
the product intent, while this file gives Manager, Discovery, implementation,
QA, and reporting roles the concrete done/not-done vocabulary.

Shared product surface names and object vocabulary live in
[Agent Desk information architecture](information-architecture.md).

## First Milestone Definition

The first milestone proves the smallest useful supervisor loop:

- sanitized runtime observations can enter Agent Desk through the accepted
  local observation-file path
- imported events can be replayed into deterministic operator state
- desktop can show a read-only timeline and decision queue from that state
- one mock or local safe action loop can be previewed, decided, recorded, and
  inspected without external side effects
- operator-visible state links to public-safe evidence, diagnostics, and known
  gaps
- local verification, public hygiene, and role/reporting expectations are
  documented and reproducible

The first milestone is not a live runtime controller. The first accepted
OpenClaw adapter remains observation-only. Direct private runtime reads, remote
sync, multi-provider expansion, broad background daemons, and outbound action
execution are outside this milestone unless a later decision issue explicitly
approves them.

## Signals Versus Completion

The following are input signals:

- no open GitHub issues or PRs
- a clean local worktree
- a green PR or green CI run
- a successful sanitized observation import
- a successful replay or smoke command
- a Discovery or Manager no-issue triage report

Completion requires those signals to be tied to the milestone criteria below.
When the issue queue is empty, Manager and Discovery must still check the
vision, recent commits, CI state, docs drift, roadmap gaps, and operator pain
before reporting that no follow-up work is warranted.

## Milestone States

### Done

Agent Desk may report the first milestone as done only when all required
criteria are satisfied.

Required evidence:

- runtime evidence: sanitized observation contract, import diagnostics, and
  canonical replay all pass representative fixture checks
- UI evidence: desktop timeline and decision queue render replayed state with
  empty, not-done, blocked/error, and success states
- action evidence: the mock or local safe action loop records preview,
  decision, result, and audit evidence without external side effects
- verification evidence: relevant tests, smoke commands, public hygiene, and
  coverage gates pass or have documented owner-approved exceptions
- operator evidence: docs explain how to run, inspect, recover, and decide
  whether the milestone is ready
- Discovery evidence: post-merge/no-issue triage confirms no unblocked
  milestone-critical follow-up remains

### Not Done

Agent Desk must report the milestone as not done when one or more required
criteria are missing, unknown, or only represented by a placeholder.

Typical evidence:

- sanitized import works, but replay, timeline, decision queue, or safe action
  evidence is missing
- a UI surface exists, but does not show required states or evidence links
- a mock action exists, but the decision/audit path is incomplete
- verification is local-only, undocumented, skipped, stale, or not mapped to
  acceptance criteria
- Discovery finds unblocked follow-up work tied to the milestone

### Empty Queue

An empty queue means there is no currently tracked GitHub work. It does not
mean product completion.

Required behavior:

- Manager runs no-issue triage against the vision, roadmap, docs, recent
  changes, CI state, and operator pain
- Discovery reports either new follow-up issues or a specific reason none are
  warranted
- reports say "open issue queue drained" or "no tracked work remains" unless
  the done criteria are also satisfied

### Blocked Or Error

Agent Desk reports blocked/error when the milestone cannot move forward without
external input, unavailable access, broken tooling, failed verification, unsafe
data, or an unresolved decision.

Required evidence:

- the blocker is named without leaking private paths, credentials, channel ids,
  raw transcripts, or private runtime details
- the affected criterion is identified
- the next safe action is stated
- if Yonatan is needed, the ask is concrete

### Successful Outcome

A successful individual slice or replay is not the same as milestone done. It
means the slice achieved its stated acceptance criteria and produced evidence
that may contribute to milestone completion.

Required evidence:

- issue or slice number
- changed behavior or docs
- tests, smoke checks, CI, or manual evidence
- public-safety and architecture boundary status
- Discovery output with follow-up issues or a reason none are warranted

## Criteria Map

| Area | Required outcome | Required evidence |
| --- | --- | --- |
| Runtime import | Sanitized observations import through the accepted local file path | fixture tests, import diagnostics, smoke output |
| Replay | Imported observations produce deterministic replay/operator state | replay tests, canonical replay command, projection evidence |
| Desktop timeline | Operator can scan what changed and current state | UI smoke/screenshot, empty/error/not-done/success states |
| Decision queue | Operator can see what needs human choice | queue projection tests, UI evidence, evidence links |
| Safe action loop | One mock/local action can be previewed, decided, recorded, and inspected | action tests, audit entry, no external side-effect proof |
| Evidence | Claims link to inspectable public-safe references | detail/evidence UI or CLI output, sanitized fixture examples |
| Verification | Acceptance criteria map to reproducible checks | tests, smoke commands, public hygiene, CI or documented local evidence |
| Reporting | Loop reports distinguish queue state from product completion | report examples, Discovery/no-issue triage output |
| Public safety | No private runtime data crosses into public artifacts | hygiene scan, negative tests, docs and fixture policy |

## Follow-Up Issue Language

Use these phrases when later slices depend on the completion criteria:

- General slices: "This slice implements or verifies one part of the milestone
  completion criteria; it does not by itself establish Agent Desk product
  completion."
- Architecture/domain slices: "This slice must preserve the completion-state
  model and evidence requirements defined here."
- UI/design slices: "This slice must expose states and evidence using the
  milestone vocabulary: done, not done, empty queue, blocked/error, successful
  outcome."
- Verification/docs/tooling slices: "This slice must provide public-safe
  evidence that can be used to evaluate the milestone criteria."

## Architecture Boundaries

Completion is a derived product state, not a stored runtime fact from OpenClaw
or any adapter.

- Observation import is only an input signal.
- Empty issue queues, Discovery output, successful imports, and successful
  smoke runs are only evidence until they are mapped to milestone criteria.
- Runtime adapters feed sanitized observations into shared app/domain
  contracts; UI consumes projections and criteria results rather than raw
  adapter observations.
- Evidence must stay public-safe: commits, checks, artifacts, sanitized
  fixtures, screenshots, smoke output, and manual verification notes with
  owner/date/context are acceptable when they contain no private details.
- Manual verification can supplement automated checks, but should not replace
  tests or smoke checks where automation is feasible.
- Safe action readiness can be defined here, but outbound action execution
  remains future-gated until a separate approved issue explicitly scopes it.

## Current Non-Goals

- live OpenClaw/runtime database reads
- raw transcript, chat log, screenshot, browser, email, or filesystem ingestion
- remote sync or hosted service behavior
- multi-provider normalization beyond the current sanitized observation contract
- team accounts, RBAC, billing, org tenancy, or cloud audit retention
- outbound sends, public posts, purchases, destructive actions, or account and
  credential changes
- autonomous remediation, automatic merges, or broad policy engines

These can become future roadmap items only through explicit decision or slice
issues with public-safe boundaries and owner approval where needed.
