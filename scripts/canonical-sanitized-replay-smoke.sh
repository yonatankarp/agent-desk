#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP_PARENT="${TMPDIR:-/tmp}"
SMOKE_DIR="$(mktemp -d "${TMP_PARENT%/}/agent-desk-canonical-replay.XXXXXX")"

cleanup() {
  rm -rf "$SMOKE_DIR"
}
trap cleanup EXIT

OBSERVATIONS_FILE="$ROOT_DIR/app/src/jvmTest/resources/openclaw/sanitized-observations.json"
EVENT_STORE="$SMOKE_DIR/events.ndjson"
CONFIG_FILE="$SMOKE_DIR/agent-desk.config.properties"

run_cli() {
  (cd "$ROOT_DIR" && ./gradlew -q :cli:run --args="$*")
}

assert_contains() {
  local haystack="$1"
  local needle="$2"
  if [[ "$haystack" != *"$needle"* ]]; then
    printf 'Expected output to contain: %s\n' "$needle" >&2
    printf 'Actual output:\n%s\n' "$haystack" >&2
    exit 1
  fi
}

assert_file_exists() {
  local path="$1"
  if [[ ! -s "$path" ]]; then
    printf 'Expected canonical replay file to exist and be non-empty.\n' >&2
    exit 1
  fi
}

printf 'Canonical sanitized replay scenario\n'

import_output="$(run_cli import-openclaw-observations --observations "$OBSERVATIONS_FILE" --event-store "$EVENT_STORE")"
assert_contains "$import_output" "Imported 10 sanitized observation event(s); skipped 0 duplicate event(s)."
assert_contains "$import_output" "Diagnostics: imported=10 skipped-duplicate=0 invalid=0 unsafe-rejected=0 store-rejected=0 redacted-or-dropped=0."
assert_file_exists "$EVENT_STORE"

duplicate_output="$(run_cli import-openclaw-observations --observations "$OBSERVATIONS_FILE" --event-store "$EVENT_STORE")"
assert_contains "$duplicate_output" "Imported 0 sanitized observation event(s); skipped 10 duplicate event(s)."
assert_contains "$duplicate_output" "Diagnostics: imported=0 skipped-duplicate=10 invalid=0 unsafe-rejected=0 store-rejected=0 redacted-or-dropped=0."

cat >"$CONFIG_FILE" <<EOF
mode=stored-events
source=local-event-store
eventStoreLocation=$EVENT_STORE
EOF

render_output="$(run_cli --config "$CONFIG_FILE")"
assert_contains "$render_output" "Agent Desk"
assert_contains "$render_output" "Current work"
assert_contains "$render_output" "- [Blocked] agent-task:211 Add sanitized export fixture"
assert_contains "$render_output" "- [Needs decision] agent-task:212 Choose fixture review path"
assert_contains "$render_output" "- [Succeeded] agent-task:213 Verify fixture success path"
assert_contains "$render_output" "- [Failed] agent-task:214 Verify fixture failure path"
assert_contains "$render_output" "- [Canceled] agent-task:215 Verify fixture cancel path"
assert_contains "$render_output" "Attention queue"
assert_contains "$render_output" "agent-task:211 Add sanitized export fixture (Blocked)"
assert_contains "$render_output" "agent-task:212 Choose fixture review path (Needs decision)"
assert_contains "$render_output" "Recent events"
assert_contains "$render_output" "Runtime adapter decision"

blocked_output="$(run_cli inspect agent-task:211 --config "$CONFIG_FILE")"
assert_contains "$blocked_output" "Work item agent-task:211"
assert_contains "$blocked_output" "Status: Blocked"
assert_contains "$blocked_output" "Attention: yes"
assert_contains "$blocked_output" "Terminal: no"
assert_contains "$blocked_output" "Accepted recent events"
assert_contains "$blocked_output" "Evidence references"
assert_contains "$blocked_output" "Runtime adapter decision"

decision_output="$(run_cli inspect agent-task:212 --config "$CONFIG_FILE")"
assert_contains "$decision_output" "Work item agent-task:212"
assert_contains "$decision_output" "Status: Needs decision"
assert_contains "$decision_output" "Attention: yes"
assert_contains "$decision_output" "Terminal: no"

success_output="$(run_cli inspect agent-task:213 --config "$CONFIG_FILE")"
assert_contains "$success_output" "Work item agent-task:213"
assert_contains "$success_output" "Status: Succeeded"
assert_contains "$success_output" "Attention: no"
assert_contains "$success_output" "Terminal: yes"

printf 'Replay evidence: timeline-ready=yes decision-queue-ready=yes not-done-state=blocked-and-needs-decision diagnostics=public-safe.\n'
printf 'Completion interpretation: no-issue/Discovery output is a triage signal, not product completion.\n'
printf 'Canonical sanitized replay scenario passed.\n'
