# Mobile Read-Only Contract

The first mobile proof uses `MobileOperatorStateContract` from `:app` as its public read model. The contract is derived from existing `OperatorState` projections and presenter labels, so mobile clients do not need to import CLI, desktop, runtime adapter, persistence, or core projection internals.

The read model exposes:

- current non-terminal work with status labels and tones
- a read-only attention queue for `NeedsDecision`, `Blocked`, and stale running or waiting work
- compact evidence references already validated by the domain model
- recent accepted event lines, each carrying its adapter-neutral source
- projection warnings for ignored stored events (the single warnings channel; store-read/import diagnostics join it when a store-backed loader feeds mobile)
- a read-only timeline derived from the same `ReadOnlyTimelineProjector` the
  shared app layer owns: entries with source, time window, type, status and
  state labels, summaries, and evidence, plus the projection's status markers
- per-entry evidence detail with source, timestamp, summary, provenance
  (`replay event <id>`), evidence references, and related events for the same
  work item — mirroring the desktop drilldown's sanitized field set; raw
  provider payloads are never part of the model

The first Compose mobile screenshot or smoke evidence should show only sanitized sample or stored-event state: current work, attention queue, stale markers when present, compact evidence labels or targets, timeline entries and evidence detail when present, and projection warnings when present. It must not include action controls, local file paths, channel IDs, raw transcripts, credentials, private screenshots, or runtime-internal identifiers.

## Capability Matrix

Capabilities by surface. "Mobile future action" rows stay unavailable until an
owner decision wires mobile actions; nothing in the current mobile client can
trigger an external side effect.

| Capability | Desktop today | Mobile today | Mobile future |
| --- | --- | --- | --- |
| Current work and attention queue | read-only view | read-only view | read-only |
| Timeline scanning with state markers | read-only view | read-only view | read-only |
| Evidence detail (source, timestamp, provenance, related items) | read-only drilldown | read-only inline expand | read-only |
| Stale markers and projection warnings | read-only view | read-only view | read-only |
| Mock resume via gated act loop | CLI only (desktop renders results) | unavailable | candidate action, gated |
| Stop / destructive controls | unavailable (fail-closed) | unavailable | owner decision required |
| External sends, posts, approvals with side effects | unavailable (fail-closed) | unavailable | owner decision required |

Suggested verification for the shared contract:

```bash
./gradlew :app:build
```

