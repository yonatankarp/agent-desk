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

CI runs `bash scripts/compose-run-smoke.sh` to exercise both Compose run tasks without opening long-lived windows.

The smoke tests cover:

- the `Current work`, `Recent events`, and `Attention queue` sections
- public-safe sample state
- empty state rows
- attention-needed rows
- loaded, loading, and invalid-input states

This is not a replacement for future screenshot or interaction tests. When the desktop shell loads non-sample state or gains actions, add UI-level verification around those workflows.
