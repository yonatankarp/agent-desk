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

## Desktop Wiring

The desktop shell uses sample mode when launched without arguments:

```bash
./gradlew :desktop:run
```

To render derived state from a sanitized local event store, pass the same config file shape:

```bash
./gradlew :desktop:run --args='--config agent-desk.config.properties'
```

The desktop UI labels sample data as `Sample state` and stored event data as `Loaded state`. Invalid config or unreadable event stores render a public-safe error state without echoing private local paths or raw adapter details.

## Mobile Wiring

The runnable mobile JVM dev host is sample-only today:

```bash
./gradlew :mobile:run
```

It starts the read-only mobile shell with built-in public-safe sample state and does not accept `--config` yet. Stored-event runtime configuration is currently wired through CLI and desktop only; add mobile config support in a later slice if mobile needs to demonstrate loaded stored-event state directly.

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

Run the repeatable public-safe smoke workflow with:

```bash
bash scripts/mock-runtime-smoke.sh
```

The smoke creates a temporary sanitized event store, imports the mock runtime source, renders configured operator state, inspects `agent-task:45`, records a mock `resume` action, verifies the action evidence, and removes its temporary files before exiting.

## Mock Operator Action

The first operator action loop is also mock-only and local-store backed:

```bash
./gradlew :cli:run --args='act resume agent-task:45 --event-store agent-desk-events.ndjson'
```

The command reads the configured event store, projects current operator state, verifies that the selected work item exists and is resumable, then appends a sanitized `work.started` result event from `mock-action-adapter`. The mock adapter currently accepts only `resume`; unsupported intents such as `stop` are rejected with public-safe errors instead of touching any private runtime integration.

Action result evidence uses sanitized note targets such as `mock-action:resume`. It must not include local paths, service identifiers, raw transcripts, channel ids, or private runtime details.
