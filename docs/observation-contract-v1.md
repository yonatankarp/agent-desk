# Observation Contract v1

Observation contract v1 is the public-safe import boundary for runtime
observations. It describes the sanitized data that local adapters may pass into
Agent Desk before canonical `WorkEvent` records are appended to an event store.

The first concrete adapter is the OpenClaw sanitized observation-file adapter.
That adapter is only one source for this contract; the contract itself is
adapter-neutral and public-safe.

## Export Shape

A sanitized export document contains:

```json
{
  "schemaVersion": 1,
  "observations": []
}
```

- `schemaVersion`: required integer. Version `1` is the only supported version.
- `observations`: required list of observation records. Records are processed
  in list order.

Unknown export or observation fields are rejected. Rejection messages stay
public-safe and do not echo raw input values.

## Observation Fields

Each observation record may contain only these fields:

| Field | Required | Meaning |
| --- | --- | --- |
| `eventId` | yes | Public-safe canonical event id such as `event:agent-task:42:started`. |
| `occurredAt` | yes | RFC 3339 UTC timestamp. |
| `source` | yes | Adapter-neutral source label such as `openclaw-local`. |
| `workItemId` | yes | Public-safe work alias such as `agent-task:42`. |
| `kind` | yes | Lifecycle observation kind. |
| `title` | required for `started` | Short public-safe work title. |
| `summary` | optional for `started` | Short public-safe operational summary. |
| `reason` | required for `needs-decision`, `blocked`, and `failed`; optional for `canceled` | Public-safe reason text. |
| `evidenceReferences` | optional | Compact public-safe evidence references. |

Accepted `kind` values are:

- `started`
- `needs-decision`
- `blocked`
- `succeeded`
- `failed`
- `canceled`

## Evidence References

Each evidence reference contains:

- `kind`: one of the existing evidence kinds accepted by the domain model, such
  as `commit`, `check-run`, `artifact`, `screenshot`, or `sanitized-note`
- `label`: short public-safe label
- `target`: public-safe target, such as a GitHub URL, artifact id, or sanitized
  documentation reference

Evidence must point to inspectable material without storing raw private
evidence in the observation record.

## Validation Behavior

The import path rejects malformed, unsupported, missing, or unsafe records
before appending canonical events.

- Malformed JSON, unsupported schema versions, unknown fields, missing required
  fields, and unsupported `kind` values fail the sanitized source load.
- Unsafe ids, unsafe text, private-looking paths, raw transcript markers,
  channel/message ids, private runtime/session ids, credentials, private URLs,
  and unsafe evidence references fail before append.
- Duplicate canonical event ids are skipped and reported as duplicates.
- Store read/write failures fail with public-safe store messages.
- Redaction or field dropping is not performed in v1. Adapters must provide
  sanitized values before import; the current diagnostic count for
  redacted/dropped records is therefore normally zero.

Failure messages and diagnostics must never echo the raw unsafe payload.

## Import Diagnostics

Successful imports return structured diagnostics for accepted and skipped
records. Failed imports carry public-safe diagnostics for rejected source or
store failures.

Diagnostic kinds are:

- `Imported`: canonical event appended
- `SkippedDuplicate`: canonical event id already existed in the target store or
  earlier in the same import
- `InvalidSource`: source file or observation shape was malformed or unsupported
- `UnsafeRejected`: observation data was rejected by public-safety validation
- `StoreRejected`: event store read or append failed
- `RedactedOrDropped`: reserved for future adapter-side redaction/drop behavior

CLI summaries include aggregate counts only. They do not print raw rejected
payloads or private file paths.

## Non-Goals

Observation contract v1 does not add:

- live sync
- direct private OpenClaw/runtime reads
- raw transcript, chat log, screenshot, browser, email, or filesystem ingestion
- multi-provider normalization beyond this minimal sanitized shape
- action execution, approval, stop, resume, retry, or cancel behavior
- redaction repair or automatic private-alias discovery
