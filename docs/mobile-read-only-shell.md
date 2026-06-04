# Mobile Read-Only Shell

The first mobile shell lives in `:mobile`. It is a Compose Multiplatform proof backed by the shared `:app` mobile read-only contract.

Current scope:

- render current work from `MobileOperatorStateContract`
- render the attention queue, including stale markers
- show compact evidence labels when the read model provides them
- show projection warnings when stored events contain ignored records
- keep stop, resume, retry, and approval actions out of the mobile UI

The module has a JVM target so CI and local agents can build and smoke-test the shared Compose surface before Android or iOS packaging is introduced. Android/iOS target wiring should be a later slice with explicit toolchain and artifact expectations.

Local verification:

```bash
./gradlew :mobile:build
```

The deterministic smoke evidence is `MobileSmokeSnapshotBuilder.sample()`, which should contain the `Current work` and `Attention queue` sections with sanitized sample work only.

