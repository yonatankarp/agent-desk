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
- Corrupt newline-terminated records fail with a line-numbered `WorkEventStoreException`.
- Reads enforce a maximum store size (10 MiB by default, constructor-configurable) and reject larger files with a public-safe error before loading any bytes.
- Error messages intentionally refer to the configured event store instead of echoing local filesystem paths, sizes, or limits.

### Torn trailing records

Appends write directly to the live file, so a crash mid-append can leave a partial final line. Reads distinguish two corruption cases:

- **Torn trailing record** — the final line is not newline-terminated and fails to decode. This is the signature of an interrupted append. `readAll()` recovers the committed prefix and returns it together with a public-safe trailing-corruption warning (literal line number and recovered event count; never path, size, or record content). History is not discarded.
- **Mid-file or newline-terminated corruption** — any other undecodable record means the write completed and the content is genuinely corrupt. Reads fail hard with a line-numbered error. The store never silently recovers past mid-file corruption, because doing so would hide tampering or data loss in an audit history.

While a torn trailing record exists, `append()` refuses with the same line-numbered corruption error and leaves the file untouched. Recovery is read-only by design: the repository never truncates or rewrites the store. Repairing a torn store is a deliberate operator action (restore from backup, or remove the torn final line manually after review).

A final record that decodes correctly but lacks its trailing newline is accepted on read; a subsequent append isolates it by writing a newline before the new record instead of corrupting it.

This is a local-first newline-delimited event file, not a transactional database. The locking behavior is intended for cooperating Agent Desk CLI, desktop, and runtime adapter processes that use the same repository implementation or otherwise honor JVM file locks. Tools that bypass locks and write directly to the file can still corrupt records mid-file; the repository fails hard on those instead of guessing.

The store accepts sanitized event records only. Private paths, credentials, channel ids, raw transcripts, and runtime-specific local identifiers must be stripped before events are appended.

## Mock Import Smoke

Use the public-safe mock runtime source to create a local store:

```bash
./gradlew :cli:run --args='import-mock-runtime --event-store agent-desk-events.ndjson'
```

Then render it through runtime config:

```bash
printf 'mode=stored-events\nsource=local-event-store\neventStoreLocation=agent-desk-events.ndjson\n' > agent-desk.config.properties
./gradlew :cli:run --args='--config agent-desk.config.properties'
```

Keep smoke files out of commits unless they are sanitized fixtures. The command writes canonical newline-delimited JSON records and skips event ids that already exist in the target store.
