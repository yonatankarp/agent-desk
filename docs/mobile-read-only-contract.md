# Mobile Read-Only Contract

The first mobile proof uses `MobileOperatorStateContract` from `:app` as its public read model. The contract is derived from existing `OperatorState` projections and presenter labels, so mobile clients do not need to import CLI, desktop, runtime adapter, persistence, or core projection internals.

The read model exposes:

- current non-terminal work with status labels and tones
- a read-only attention queue for `NeedsDecision`, `Blocked`, and stale running or waiting work
- compact evidence references already validated by the domain model
- recent accepted event lines
- projection warnings for ignored stored events

The first Compose mobile screenshot or smoke evidence should show only sanitized sample or stored-event state: current work, attention queue, stale markers when present, compact evidence labels or targets, and projection warnings when present. It must not include action controls, local file paths, channel IDs, raw transcripts, credentials, private screenshots, or runtime-internal identifiers.

Suggested verification for the shared contract:

```bash
./gradlew :app:build
```

