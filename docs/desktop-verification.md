# Desktop Verification

The Compose desktop shell is verified with a CI-ready headless smoke snapshot. The snapshot mirrors the main UI sections through the shared operator presenter instead of relying on a display server or brittle screenshot comparison.

Run it locally with:

```bash
./gradlew :desktop:build
```

The smoke tests cover:

- the `Current work`, `Recent events`, and `Attention queue` sections
- public-safe sample state
- empty state rows
- attention-needed rows

This is not a replacement for future screenshot or interaction tests. When the desktop shell loads non-sample state or gains actions, add UI-level verification around those workflows.
