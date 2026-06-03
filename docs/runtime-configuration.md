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

## Follow-Up Wiring

The config contract is available in shared app code. CLI file loading in #39 and desktop state loading in #40 should parse this template shape and use `LocalFileWorkEventRepository` when `mode=stored-events`.
