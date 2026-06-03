# Local Event Store

Agent Desk stores sanitized work events through the shared `WorkEventRepository` port. The first implementation is `LocalFileWorkEventRepository`, a JVM-local newline-delimited JSON store for CLI and desktop adapters.

## Shape

Each line is one compact `WorkEventJson` record:

```json
{"id":"event:agent-task:42:started","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.started","payload":{"title":"Run public hygiene check","summary":"Agent accepted the task and started local checks."}}
```

Adapters configure the storage location when constructing the repository. Do not hard-code private machine paths in source, docs, or committed fixtures.

Runtime config rules for choosing this store are described in [Runtime configuration](runtime-configuration.md).

```kotlin
val repository = LocalFileWorkEventRepository(Path.of("agent-desk-events.ndjson"))
```

## Behavior

- Missing files and empty stores read as an empty event stream.
- Appends create the parent directory when one is configured.
- Duplicate event ids are rejected on append and when reading an existing store.
- Corrupt records fail with a line-numbered `WorkEventStoreException`.
- Error messages intentionally refer to the configured event store instead of echoing local filesystem paths.
- A repository instance caches event ids for append-time duplicate checks. Create a new repository instance or call `readAll()` after another process mutates the file.

The store accepts sanitized event records only. Private paths, credentials, channel ids, raw transcripts, and runtime-specific local identifiers must be stripped before events are appended.
