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

The deterministic smoke evidence is `MobileSmokeSnapshotBuilder.sample()`, which should contain the `Current work` and `Attention queue` sections with sanitized sample work only.
