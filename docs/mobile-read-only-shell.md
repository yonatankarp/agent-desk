# Mobile Read-Only Shell

The first mobile shell lives in `:mobile`. It is a Compose Multiplatform proof backed by the shared `:app` mobile read-only contract.

Current scope:

- render current work from `MobileOperatorStateContract`
- render the attention queue, including stale markers
- show compact evidence labels when the read model provides them
- show projection warnings when stored events contain ignored records
- keep stop, resume, retry, and approval actions out of the mobile UI

The shared mobile read model can represent projection warnings from stored events, and the smoke snapshot covers that read-only contract. The runnable JVM dev host is sample-only today: `./gradlew :mobile:run` starts `AgentDeskMobileApp()` with built-in public-safe sample state and does not accept `--config` yet.

The module has a JVM target so CI and local agents can build and smoke-test the shared Compose surface before Android or iOS packaging is introduced. Android/iOS target wiring should be a later slice with explicit toolchain and artifact expectations.

Local verification:

```bash
bash scripts/mobile-read-only-smoke.sh
bash scripts/compose-run-smoke.sh
./gradlew :mobile:build
```

Run the non-interactive mobile run-task smoke directly:

```bash
./gradlew :mobile:run --args='--smoke-exit'
```

Run the sample-only shell in a phone-sized desktop window:

```bash
./gradlew :mobile:run
```

The deterministic smoke evidence is `MobileSmokeSnapshotBuilder.sample()`, which should contain the `Current work`, `Attention queue`, and `Timeline` sections with sanitized sample work only.

## Timeline and Evidence Detail

The shell renders a read-only `Timeline` section below recent events: a status
line with the projection's state markers, entries grouped by time window, and
per-entry rows that stack timestamp, type, work item id, state label, summary,
and evidence vertically so dense data wraps instead of scrolling horizontally
(a Konsist rule forbids `horizontalScroll` in mobile production code).

Each timeline entry is a disclosure: tapping the row toggles an inline
`Details` block showing the sanitized evidence detail (source, timestamp,
summary, provenance as `replay event <id>`, evidence references, related
events) plus the redaction line stating raw provider payloads are never
rendered. The disclosure is deliberately not styled as a button — nothing on
the mobile surface is an action. The capability matrix in
[Mobile read-only contract](mobile-read-only-contract.md) records which
capabilities are desktop-only, mobile read-only, or future mobile actions.
