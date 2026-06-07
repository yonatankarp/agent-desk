# Local Audit Store

Agent Desk persists permission decisions and approval-loop outcomes through the
`AuditRecordRepository` port. The first implementation is
`LocalFileAuditRecordRepository`, a JVM-local newline-delimited JSON store that
mirrors the [local event store](local-event-store.md) mechanics, kept as a
separate file because audit records and work events are different domain
streams with different projections.

## Shape

Each line is one compact `AuditRecordJson` record of a validated `AuditEntry`:

```json
{"id":"audit:agent-task:42:resume:decision:2026-06-02T21:22:00Z","actor":"operator:daily-agent","actorKind":"human","timestamp":"2026-06-02T21:22:00Z","recordedAt":"2026-06-02T21:23:00Z","action":"decision.approve-resume","target":"agent-task:42","result":"approved","sourceItem":"agent-task:42","correlationId":"correlation:agent-task:42:resume:2026-06-02T21:22:00Z","evidenceReference":{"kind":"sanitized-note","label":"Mock action Approved","target":"mock-action:resume:approved"},"detail":"Public-safe mock approval."}
```

`timestamp` is the time of the underlying fact (decision time for decision
entries, action-event time for executed actions); `recordedAt` is the audit
record's own time. A non-executing outcome (denial, deferral, cancellation)
has no action event, so its action entry carries `recordedAt` explicitly
instead of silently reusing the decision time.

Timestamps persist in the canonical `EventTimestamp` form (fractional-second
trailing zeros trimmed). Audit and correlation ids interpolate that canonical
form, so idempotency keys are stable across writers that format the same
instant at different precision.

Adapters configure the storage location when constructing the repository. Do
not hard-code private machine paths in source, docs, or committed fixtures.

## Behavior

Identical to the [local event store](local-event-store.md) discipline:

- Missing files and empty stores read as an empty trail.
- Appends create the parent directory, serialize by normalized store path
  inside the JVM, and hold a cooperative exclusive file lock while writing.
- Duplicate record ids are rejected on append and on read. Duplicate errors
  carry only the line number — never the record id, path, or content.
- Decoding reconstructs every field through its value-object `parse()` and the
  `AuditEntry` validation, so a hand-edited store line cannot smuggle
  non-public-safe content past load; it fails as a corrupt record instead.
- Torn trailing records (interrupted appends) recover the committed prefix and
  block further appends until the store is repaired; mid-file or
  newline-terminated corruption fails hard with a line-numbered error. Repair
  is a deliberate read-only operator action — see the torn-record section of
  the [local event store](local-event-store.md) for the recovery guidance.
- Reads enforce a maximum store size (10 MiB by default,
  constructor-configurable).
- Error messages refer to the configured audit store and never echo paths,
  sizes, ids, or record content.

## Recording

`ActionPermissionGate.decide()` and `MockActionApprovalLoop.decide()` stay
pure. Callers route their outcomes through `AuditTrailRecorder`, which projects
the decision or approval result into audit entries and appends them to the
store. The CLI `act` command is the first production caller: it routes every
invocation through the gate and records approvals, denials, and unsupported
outcomes via the recorder (see
[Runtime configuration](runtime-configuration.md), Mock Operator Action).

The store accepts validated public-safe audit records only. Private paths,
credentials, channel ids, raw transcripts, and runtime-specific local
identifiers must never reach an append.

## Reading

`AuditRecordRepository.readAll()` is the read seam: it returns the committed
entries plus an optional public-safe torn-trailing-record signal, and never
mutates the store. The CLI `report` command is the operator-reachable read
consumer: `agent-desk report <work-item-id> --events <file> --audit-store
<file>` renders the work item's readiness projection and the recorded gate,
loop, and action records grouped by correlation id. Read errors surface the
store's public-safe messages (line numbers and failure class only, never the
path or record content).
