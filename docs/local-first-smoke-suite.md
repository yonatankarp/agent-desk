# Local-First Smoke Suite

The local-first smoke suite verifies the core Agent Desk loop on a clean
checkout without external services, accounts, network APIs, or live provider
writes. It composes existing public-safe smoke scripts instead of adding a
second smoke framework.

Run it with:

```bash
make smoke-local-first
```

If the shell does not have Java on `PATH`, set `JAVA_HOME` before running the
target. A missing Java runtime is a local setup failure, not a product
regression.

Equivalent expanded commands:

```bash
bash scripts/validate-public-hygiene.sh
bash scripts/canonical-sanitized-replay-smoke.sh
bash scripts/mock-runtime-smoke.sh
```

## Coverage

The suite covers:

- public hygiene for tracked files
- sanitized observation import from the checked-in fixture
- canonical replay into a temporary local event store
- duplicate import handling and aggregate diagnostics
- stored-event operator rendering
- timeline-ready and decision-queue-ready evidence
- inspection of blocked, needs-decision, succeeded, failed, and canceled work
- mock/local `resume` action path and sanitized action evidence
- cleanup of temporary local event stores and config files

## Dependency Boundary

The suite must not require:

- external network access
- account credentials
- live provider APIs
- local private runtime exports
- screenshots or browser state
- persistent local state outside the temporary smoke directory

The checked-in sanitized fixture and mock runtime source are the only data
sources. Any future expansion that requires external access should be a separate
manual validation step, not part of `make smoke-local-first`.

## Failure Interpretation

Missing local setup:

- Gradle wrapper cannot run
- JDK is unavailable or incompatible
- shell cannot create a temporary directory
- executable permissions are missing on smoke scripts

Product regression:

- public hygiene fails
- sanitized observations no longer import
- duplicate diagnostics change unexpectedly
- stored-event rendering omits current work, timeline, decision queue, or evidence
- mock resume action fails or emits unsafe evidence
- temporary files are not cleaned up

Failures must print the failing expected output and actual public-safe output.
Do not paste raw local paths, private runtime exports, or private tool output
into issues. Summarize the failed command and public-safe assertion instead.

## Release Readiness

This suite feeds release readiness as local smoke evidence. It does not replace
CI-required gates, platform builds, coverage checks, or manual UI review for UI
changes. Record the command result in PRs and release readiness reports as:

```text
make smoke-local-first
```
