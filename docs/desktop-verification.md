# Desktop Verification

The Compose desktop shell is verified with a CI-ready headless smoke snapshot. The snapshot mirrors the main UI sections through the shared operator presenter instead of relying on a display server or brittle screenshot comparison.

Run it locally with:

```bash
./gradlew :desktop:build
```

Run the non-interactive desktop run-task smoke:

```bash
./gradlew :desktop:run --args='--smoke-exit'
```

Run the desktop shell in public-safe sample mode with:

```bash
./gradlew :desktop:run
```

Run it against a sanitized local event store by passing the shared runtime configuration file:

```bash
./gradlew :desktop:run --args='--config agent-desk.config.properties'
```

The config file uses the same public-safe values documented in [Runtime configuration](runtime-configuration.md). Sample mode is labelled `Sample state`; stored event mode is labelled `Loaded state`; invalid config or unreadable stores render a public-safe error state instead of echoing raw local details.

The first read-only operator surface shows `Replay status`, `Work state`, `Read-only timeline`, and `Decision queue`. `Replay status` is deliberately interpretive: it distinguishes empty queue from product completion, calls out not-done states when attention is required, points import diagnostics back to the canonical replay smoke, and treats Discovery/no-issue output as triage only. The desktop timeline is rendered from the shared read-only timeline projection, so smoke evidence includes projection status markers, time-window grouping, per-entry state labels, completion outcomes, sources, and sanitized evidence references. Surface names and object vocabulary are defined in [Agent Desk information architecture](information-architecture.md).

CI runs `bash scripts/compose-run-smoke.sh` to exercise both Compose run tasks without opening long-lived windows.

The smoke tests cover:

- the `Replay status`, `Work state`, `Read-only timeline`, and `Decision queue` sections
- public-safe sample state
- empty state rows
- not-done and attention-needed rows
- evidence references on current-work rows and stale metadata on attention rows
- timeline projection status markers, grouped dates, completion outcomes, and evidence references
- loaded, loading, and invalid-input states

This is not a replacement for future screenshot or interaction tests. When the desktop shell loads non-sample state or gains actions, add UI-level verification around those workflows.
