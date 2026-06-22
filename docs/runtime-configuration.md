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

## Sanitized Observation Import

The first OpenClaw runtime adapter path imports an operator-provided sanitized observation export:

```bash
./gradlew :cli:run --args='import-openclaw-observations --observations sanitized-observations.json --event-store agent-desk-events.ndjson'
```

The command reads only the sanitized export file, maps observations through the shared runtime boundary, and appends canonical events into the local event store. It does not read private runtime databases, raw transcripts, local process state, private screenshots, or action-control state.

The initial checked-in fixture lives at `app/src/jvmTest/resources/openclaw/sanitized-observations.json`. It is synthetic and public-safe; do not replace it with copied private runtime logs.

Run the repeatable public-safe smoke workflow with:

```bash
make replay-sanitized-runtime
```

The canonical replay smoke creates a temporary sanitized event store, imports the checked-in sanitized fixture, verifies duplicate skipping and aggregate diagnostics, renders configured operator state, inspects blocked, needs-decision, and succeeded work, verifies sanitized evidence output, and removes its temporary files before exiting. The older `make smoke-sanitized-runtime` target runs the same scenario for compatibility with the full smoke suite.

## Mock Operator Action

The first operator action loop is also mock-only and local-store backed, and it
routes through the permission gate, approval loop, and durable audit trail:

```bash
./gradlew :cli:run --args='act resume agent-task:45 --event-store agent-desk-events.ndjson --audit-store agent-desk-audit.ndjson --approve'
```

The command reads the configured event store, projects current operator state,
plans a proposal for the selected work item, and routes it through the
permission gate. Without `--approve` the gate denies the local action, the
denial is appended to the configured audit store, and the command exits with
code 3 (policy denied; distinct from input errors). With `--approve` the
approval loop performs the mock resume, appends a sanitized `work.started`
result event from `mock-action-adapter` (with a per-invocation unique event
id), and records the decision and outcome to the audit store described in
[Local audit store](local-audit-store.md). The mock adapter currently accepts
only `resume`; unsupported intents such as `stop` are denied fail-closed with
public-safe output and still produce audit evidence.

Store files are runtime artifacts; `*.ndjson` is gitignored so local stores
never enter the public repository.

Action result evidence uses sanitized note targets such as `mock-action:resume`. It must not include local paths, service identifiers, raw transcripts, channel ids, or private runtime details.

## Local Host Profiles

Live-host diagnostics use a separate local host profile instead of the dashboard
runtime configuration. Host profiles are runtime configuration, not project
source. Keep real profiles outside the repository or in ignored
`agent-desk.host*.properties` files.

Tracked docs may show only placeholders and public-safe aliases:

```properties
hostAlias=operator-lab
hostEndpoint=<local-http-or-https-endpoint>
hostAliasMappings=runtime-primary=operator-lab
```

`hostAlias` is the only host identifier intended for public output. It is
validated with the same public-safe alias policy used by reachability
diagnostics. `hostEndpoint` is local-only private configuration; renderers and
diagnostics must report it as local-only and must not print hostnames, IP
addresses, ports, socket paths, credentials, or raw parser exceptions. Optional
`hostAliasMappings` entries let an operator map local runtime ids to
public-safe aliases explicitly instead of deriving aliases from private ids.

Do not paste real host profile values into public issues, PRs, CI logs,
screenshots, or release notes. Public evidence should cite only the command
name, public-safe alias, diagnostic category, and sanitized test output.

## Host Auth And Permission Boundary

Host connection auth is modeled separately from network reachability. Public
state may show only the host alias, auth state, permission mode, and allowed
operation classes.

Auth states:

- `not-configured`: no local auth material is configured.
- `pending`: a local pairing or approval step is not complete.
- `accepted`: local auth is present and accepted for the configured host.
- `rejected`: the host rejected the local auth material.
- `expired`: previously accepted local auth is no longer valid.
- `unsupported`: the host or mode cannot support this connection.

Permission modes:

- `diagnostic-only`: may perform reachability diagnostics after auth is
  accepted.
- `read-only-observation`: may perform diagnostics and read sanitized
  observations after auth is accepted.
- `action-capable`: may perform diagnostics, read sanitized observations, and
  propose an inspect action after auth is accepted.
- `unsupported`: allows no host operations.

Mutating live actions such as stop, resume, retry, and cancel are not allowed by
any host permission mode in the current milestone. They remain deferred until
the live action proposal and approval flow is explicitly accepted.

Credential references are local-only configuration. Tracked docs, tests, CI
output, issues, PRs, screenshots, and release notes must not include tokens,
authorization headers, one-time codes, account ids, raw endpoint details, or
private parser errors.

## Host Reachability Smoke

Run the local host reachability smoke with an ignored host profile:

```bash
./gradlew :cli:run --args='host-smoke --host-config agent-desk.host.properties'
```

The command reads `hostAlias` and `hostEndpoint` from the local profile, checks
the configured host from the local machine, and prints only a public-safe
reachability diagnostic. A reachable host exits `0`; missing configuration,
unreachable hosts, timeouts, auth rejection, unsupported mode, or redacted
unsafe details exit non-zero.

Example public-safe output:

```text
Host reachability: host=host:primary state=unreachable failure=network-unavailable.
```

Do not paste the real host profile, hostname, IP address, port, URL, token,
authorization header, socket path, or raw command failure into public issues or
PR comments. Public evidence should include only the command name, sanitized
host alias, diagnostic state, failure category, and the local/CI check names.
