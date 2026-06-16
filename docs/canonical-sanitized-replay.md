# Canonical Sanitized Replay

The canonical sanitized replay scenario is the first repeatable proof that a
public-safe observation export can become Agent Desk operator state without live
sync, private runtime reads, or outbound actions.

Run it with:

```bash
make replay-sanitized-runtime
```

The command uses the checked-in sanitized fixture at
`app/src/jvmTest/resources/openclaw/sanitized-observations.json`.

## What It Proves

The scenario verifies that:

- observation contract v1 input imports through the sanitized file adapter
- import diagnostics report imported and duplicate records with aggregate
  public-safe counts
- the local event store can replay the imported observations into rendered
  operator state
- rendered state is timeline-ready because it includes current work and recent
  events
- rendered state is decision-queue-ready because blocked and needs-decision work
  appear in the attention queue
- evidence references survive into inspectable output without storing private
  source material
- public-safe provenance aliases survive observation import, event-store replay,
  and read-only timeline/mobile projections without exposing private runtime
  identifiers
- the fixture covers blocked, needs-decision, succeeded, failed, and canceled
  states

The scenario prints:

```text
Replay evidence: timeline-ready=yes decision-queue-ready=yes not-done-state=blocked-and-needs-decision diagnostics=public-safe.
Completion interpretation: no-issue/Discovery output is a triage signal, not product completion.
Canonical sanitized replay scenario passed.
```

Those lines are intentionally stable enough for local smoke, CI-adjacent
checks, and release-readiness evidence.

## How To Interpret It

Passing replay means the sanitized import path can produce deterministic
operator state from a public-safe local event store. It does not mean Agent Desk
is product-complete.

Per the milestone completion criteria, an empty issue queue, Discovery output,
successful observation import, or successful replay is a signal. Product
completion still requires the milestone state and evidence matrix in
[Agent Desk milestone completion criteria](milestone-completion-criteria.md).

## Non-Goals

This scenario does not add:

- live sync
- direct private OpenClaw/runtime reads
- raw transcript, browser, email, screenshot, or filesystem ingestion
- remote ingestion or multi-provider adapter expansion
- stop, resume, approval, retry, cancel, or outbound action execution
