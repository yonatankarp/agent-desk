# Mobile Display-Parity Contract

Desktop and mobile display functionality are kept aligned in both directions.
Mobile may present the same information more compactly, but it should not omit
display capabilities merely because it is mobile. The intentional divergence is
side-effecting action controls: stop, resume, retry, approve, external sends, and
other mutations remain unavailable until an owner-approved action path defines
proposal, approval, audit, and executor behavior.

The mobile surface uses `MobileOperatorStateContract` from `:app` as its public
read model. The contract is derived from existing `OperatorState` projections
and presenter labels, so mobile clients do not need to import CLI, desktop,
runtime adapter, persistence, or core projection internals.

The read model exposes:

- current non-terminal work with status labels and tones
- a display-only attention queue for `NeedsDecision`, `Blocked`, and stale running or waiting work
- compact evidence references already validated by the domain model
- recent accepted event lines, each carrying its adapter-neutral source
- projection warnings for ignored stored events (the single warnings channel; store-read/import diagnostics join it when a store-backed loader feeds mobile)
- a display-only timeline derived from the same `ReadOnlyTimelineProjector` the
  shared app layer owns: entries with source, time window, type, status and
  state labels, summaries, and evidence, plus the projection's status markers
- per-entry evidence detail with source, timestamp, summary, provenance
  (`replay event <id>`), evidence references, and related events for the same
  work item — mirroring the desktop drilldown's sanitized field set; raw
  provider payloads are never part of the model

The first Compose mobile screenshot or smoke evidence should show only sanitized
sample or stored-event state: current work, attention queue, stale markers when
present, compact evidence labels or targets, timeline entries and evidence
detail when present, and projection warnings when present. It must not include
action controls, local file paths, channel IDs, raw transcripts, credentials,
private screenshots, or runtime-internal identifiers.

## Capability Matrix

Display capabilities are equal by contract. Rows marked "in progress" describe
known implementation gaps tracked by open parity issues; they are not completed
parity claims.

| Display capability | Desktop today | Mobile today | Contract status |
| --- | --- | --- | --- |
| Current work / work state | display view | display view | equal by contract |
| Decision queue / attention queue | display view | display view | equal by contract |
| Timeline scanning with state markers | display view | display view | equal by contract |
| Evidence references on work and attention rows | display view | display view | equal by contract |
| Evidence detail fields and related items | partial | partial; #333 tracks decision/criteria detail parity | in progress |
| Timeline source | display view | display view | equal by contract |
| Stale markers and projection warnings | display view | display view | equal by contract |

Action capabilities are intentionally separate from display parity. Nothing in
the current mobile client can trigger an external side effect, and future action
rows stay unavailable until an owner decision wires mobile actions.

| Action capability | Desktop today | Mobile today | Mobile future |
| --- | --- | --- | --- |
| Mock resume via gated act loop | CLI only (desktop renders results) | unavailable | candidate action, gated |
| Stop / destructive controls | unavailable (fail-closed) | unavailable | owner decision required |
| External sends, posts, approvals with side effects | unavailable (fail-closed) | unavailable | owner decision required |

## Structural Guard

The structural guard for display parity is the shared non-Compose display
structure source consumed by desktop, mobile, and snapshot/text renderers, with
section presence and ordering tested once. Parity docs should cite open
implementation gaps instead of claiming every field is rendered on both surfaces.

Suggested verification for the shared contract:

```bash
./gradlew :app:build
```
