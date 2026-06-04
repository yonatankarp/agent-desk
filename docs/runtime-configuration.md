# Runtime Configuration

Agent Desk runtime configuration chooses which public-safe event source should feed operator state.

## Values

- `mode`: `sample` or `stored-events`.
- `source`: `mock` or `local-event-store`.
- `eventStoreLocation`: sanitized newline-delimited JSON event store location, required only for `stored-events` with `local-event-store`.

The default configuration is sample mode with the mock source and no event store location.

## Validation

Configuration validation rejects unsafe event-store values before they reach adapters. Error messages name the invalid field and rule, but do not echo raw values that may contain private paths, credentials, service ids, raw transcripts, or other local setup details.

Stored event mode requires:

```properties
mode=stored-events
source=local-event-store
eventStoreLocation=agent-desk-events.ndjson
```

Sample mode requires:

```properties
mode=sample
source=mock
```

The checked-in template is [agent-desk.config.example.properties](agent-desk.config.example.properties). It uses placeholders and sanitized values only.

## CLI Wiring

The CLI accepts runtime configuration with:

```bash
./gradlew :cli:run --args='--config agent-desk.config.properties'
```

The CLI adapter reads the `.properties` file and passes the string map into shared app configuration parsing. Shared app code owns config validation, stored-event repository loading, and projection into operator state. CLI code owns only argv parsing, file IO for the config document, output rendering, and exit codes.

`--config` is an input mode and cannot be combined with `--sample`, `--stdin`, or `--events`.

## Mock Runtime Import

The first runtime import smoke command uses only the checked-in mock source:

```bash
./gradlew :cli:run --args='import-mock-runtime --event-store agent-desk-events.ndjson'
```

The command loads `MockRuntimeWorkEventSource`, maps sanitized observations through the shared runtime boundary, and appends canonical events through `WorkEventRepository`. Running it again against the same store skips duplicate event ids instead of writing repeated records.

Render the imported store with the normal stored-event configuration:

```properties
mode=stored-events
source=local-event-store
eventStoreLocation=agent-desk-events.ndjson
```

```bash
./gradlew :cli:run --args='--config agent-desk.config.properties'
```

This mock import path is a public-safe fixture workflow for tests and demos. It is not the later local runtime adapter; private runtime details must still be stripped by a concrete adapter before creating sanitized observations.
