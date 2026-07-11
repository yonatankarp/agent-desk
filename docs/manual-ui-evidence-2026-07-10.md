# Manual Desktop And Mobile UI Evidence - 2026-07-10

Scope: current release candidate at `7aec1f6`.

This artifact records a public-safe manual UI evidence pass for the desktop and
mobile display gates in [Roadmap verification gate matrix](roadmap-verification-gate-matrix.md).
It uses the repository's deterministic smoke snapshots and non-interactive
Compose run tasks instead of committing screenshots. The current verification
docs define those snapshots as the CI-ready evidence path for the Compose shells;
they avoid browser chrome, local paths, account data, private runtime state, and
raw provider payloads.

## Evidence Reviewed

- Desktop display surface:
  - `bash scripts/compose-run-smoke.sh` passed.
  - `./gradlew :desktop:allTests` passed.
  - `docs/desktop-verification.md` defines the current desktop evidence path as
    a headless smoke snapshot mirroring the main UI sections through the shared
    operator presenter.
  - Manual review confirmed the documented surface coverage remains aligned with
    the smoke snapshot: `Replay status`, `Work state`, `Read-only timeline`,
    `Decision queue`, and evidence drilldown rows render public-safe sample or
    sanitized stored-event state, including timeline markers, grouped entries,
    outcomes, sources, stale attention metadata, and sanitized evidence
    references.
- Mobile display surface:
  - `bash scripts/compose-run-smoke.sh` passed.
  - `bash scripts/mobile-read-only-smoke.sh` passed.
  - `./gradlew :mobile:allTests` passed.
  - `docs/mobile-read-only-shell.md` defines the current mobile evidence path as
    the deterministic `MobileSmokeSnapshotBuilder.sample()` display contract.
  - Manual review confirmed the documented surface coverage remains aligned with
    the smoke snapshot: `Projection warnings`, `Current work`, `Timeline`,
    `Attention queue`, and `Evidence detail` render read-only public-safe sample
    state with compact evidence references and no action controls.

## Public-Safety Review

- No screenshot file was committed.
- No private runtime data, local account data, channel ids, credentials, tokens,
  raw transcripts, private URLs, or raw provider payloads were copied into this
  artifact.
- Evidence references are limited to public commit ids, relative repository
  paths, and command names.
- The mobile and desktop surfaces remain read-only for this evidence pass.

## Result

Manual UI evidence gate: ready for the current release candidate.

Residual risk: this pass proves the current deterministic sample and stored-event
display paths, not a real operator account or private runtime session. That is
intentional for a public-safe release candidate.
