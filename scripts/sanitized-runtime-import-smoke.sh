#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP_PARENT="${TMPDIR:-/tmp}"
SMOKE_DIR="$(mktemp -d "${TMP_PARENT%/}/agent-desk-sanitized-smoke.XXXXXX")"

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
    printf 'Expected smoke file to exist and be non-empty.\n' >&2
    exit 1
  fi
}

import_output="$(run_cli import-openclaw-observations --observations "$OBSERVATIONS_FILE" --event-store "$EVENT_STORE")"
assert_contains "$import_output" "Imported 10 sanitized observation event(s); skipped 0 duplicate event(s)."
assert_file_exists "$EVENT_STORE"

duplicate_output="$(run_cli import-openclaw-observations --observations "$OBSERVATIONS_FILE" --event-store "$EVENT_STORE")"
assert_contains "$duplicate_output" "Imported 0 sanitized observation event(s); skipped 10 duplicate event(s)."

cat >"$CONFIG_FILE" <<EOF
mode=stored-events
source=local-event-store
eventStoreLocation=$EVENT_STORE
EOF

render_output="$(run_cli --config "$CONFIG_FILE")"
assert_contains "$render_output" "Agent Desk"
assert_contains "$render_output" "agent-task:211"
assert_contains "$render_output" "Add sanitized export fixture"
assert_contains "$render_output" "Runtime adapter decision"

inspect_output="$(run_cli inspect agent-task:211 --config "$CONFIG_FILE")"
assert_contains "$inspect_output" "Work item agent-task:211"
assert_contains "$inspect_output" "Accepted recent events"
assert_contains "$inspect_output" "Evidence references"
assert_contains "$inspect_output" "Runtime adapter decision"

printf 'Sanitized runtime import smoke passed.\n'
