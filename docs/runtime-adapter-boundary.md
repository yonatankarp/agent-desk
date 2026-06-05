# Runtime Adapter Boundary

Agent Desk imports delegated-work events through a sanitized application boundary. Runtime adapters may observe local process state, host paths, account identifiers, chat metadata, raw transcripts, or runtime-specific task ids, but those details must not cross into `:core` domain records.

## Contract

The shared `:app` module owns the first runtime import contract:

- `RuntimeWorkEventSource`: port implemented by runtime adapters that can load sanitized `RuntimeWorkObservation` records.
- `RuntimeWorkObservation`: sanitized observation DTO accepted by the mapper.
- `RuntimeWorkObservationKind`: supported observation kinds for the canonical lifecycle: `Started`, `NeedsDecision`, `Blocked`, `Succeeded`, `Failed`, and `Canceled`.
- `SanitizedRuntimeObservationMapper`: validates sanitized observations and maps them into canonical `WorkEvent` records.
- `RuntimeWorkEventImporter`: imports mapped runtime events into a `WorkEventRepository` and skips event ids already present in the target store.
- `MockRuntimeWorkEventSource`: public-safe fixture source for tests, CLI smoke work, and future demos.
- `OpenClawRuntimeObservationFileSource`: JVM-local source for operator-provided sanitized observation exports.

Adapters should strip or hash private runtime identifiers before creating `RuntimeWorkObservation`. The observation fields should already use public-safe ids and text that are suitable for docs, CI logs, and screenshots.

Payload requirements stay explicit at the boundary:

- `Started` requires `title`; `summary` is optional.
- `NeedsDecision`, `Blocked`, and `Failed` require `reason`.
- `Succeeded` has no payload text.
- `Canceled` accepts an optional `reason`.

## Private Fields

These details stay local to concrete adapters:

- local filesystem paths
- credentials, tokens, secret references, and vault URIs
- channel, account, message, thread, or participant ids
- raw transcripts and user-authored private content
- process ids, socket paths, hostnames, and machine-local runtime ids
- OpenClaw-specific internal object names unless deliberately translated into an adapter-neutral source id

The mapper rejects obvious unsafe fragments before constructing `WorkEvent` value objects. Core value objects still enforce canonical id, timestamp, source, title, and summary formats after the boundary check.

## Package Direction

The dependency direction is:

```text
concrete adapter -> :app runtime port/mapper -> :core domain model
```

`:core` must not import adapter or runtime packages. Shared application code in `:app` must not import CLI, desktop, adapter, OpenClaw, or private runtime packages. Konsist tests enforce these package directions where the code exists today.
