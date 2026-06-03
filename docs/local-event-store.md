# Local Event Store

Agent Desk stores sanitized work events through the shared `WorkEventRepository` port. The first implementation is `LocalFileWorkEventRepository`, a JVM-local newline-delimited JSON store for CLI and desktop adapters.

## Shape

Each line is one compact `WorkEventJson` record:

```json
{"id":"event:agent-task:42:started","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.started","payload":{"title":"Run public hygiene check","summary":"Agent accepted the task and started local checks."}}
```

Adapters configure the storage location when constructing the repository. Do not hard-code private machine paths in source, docs, or committed fixtures.

Runtime config rules for choosing this store are described in [Runtime configuration](runtime-configuration.md).

`WorkEventJson` lives in `com.yonatankarp.agentdesk.app.serialization`, outside the core domain boundary. The repository depends inward on domain `WorkEvent` values and uses the app serialization boundary only when reading or writing the local wire format.

```kotlin
val repository = LocalFileWorkEventRepository(Path.of("agent-desk-events.ndjson"))
```

## Behavior

- Missing files and empty stores read as an empty event stream.
- Appends create the parent directory when one is configured.
- Appends are serialized inside the current JVM by normalized store path, then protected with a cooperative exclusive file lock while the record is written.
- Duplicate event ids are rejected on append and when reading an existing store.
- Append duplicate checks re-read the current store while the append lock is held, so two repository instances in the same JVM do not depend on stale in-memory event-id caches.
- Corrupt records fail with a line-numbered `WorkEventStoreException`.
- Error messages intentionally refer to the configured event store instead of echoing local filesystem paths.

This is a local-first newline-delimited event file, not a transactional database. The locking behavior is intended for cooperating Agent Desk CLI, desktop, and runtime adapter processes that use the same repository implementation or otherwise honor JVM file locks. Tools that bypass locks and write directly to the file can still corrupt records; the repository rejects corrupt partial records before appending more data.

The store accepts sanitized event records only. Private paths, credentials, channel ids, raw transcripts, and runtime-specific local identifiers must be stripped before events are appended.
