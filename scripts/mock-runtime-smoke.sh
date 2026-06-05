#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP_PARENT="${TMPDIR:-/tmp}"
SMOKE_DIR="$(mktemp -d "${TMP_PARENT%/}/agent-desk-mock-smoke.XXXXXX")"

cleanup() {
  rm -rf "$SMOKE_DIR"
}
trap cleanup EXIT

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

import_output="$(run_cli import-mock-runtime --event-store "$EVENT_STORE")"
assert_contains "$import_output" "Imported 6 mock runtime event(s); skipped 0 duplicate event(s)."
assert_file_exists "$EVENT_STORE"

duplicate_output="$(run_cli import-mock-runtime --event-store "$EVENT_STORE")"
assert_contains "$duplicate_output" "Imported 0 mock runtime event(s); skipped 6 duplicate event(s)."

cat >"$CONFIG_FILE" <<EOF
mode=stored-events
source=local-event-store
eventStoreLocation=$EVENT_STORE
EOF

render_output="$(run_cli --config "$CONFIG_FILE")"
assert_contains "$render_output" "Current work"
assert_contains "$render_output" "- [Blocked] agent-task:44 Investigate core test failure"
assert_contains "$render_output" "- [Needs decision] agent-task:45 Choose retry strategy"
assert_contains "$render_output" "Attention queue"
assert_contains "$render_output" "agent-task:45 Choose retry strategy (Needs decision)"

inspect_output="$(run_cli inspect agent-task:45 --config "$CONFIG_FILE")"
assert_contains "$inspect_output" "Work item agent-task:45"
assert_contains "$inspect_output" "Status: Needs decision"
assert_contains "$inspect_output" "Attention: yes"
assert_contains "$inspect_output" "Projection warnings"

action_output="$(run_cli act resume agent-task:45 --event-store "$EVENT_STORE")"
assert_contains "$action_output" "Recorded resume action for agent-task:45 as event:agent-task:45:action-resume."

post_action_output="$(run_cli --config "$CONFIG_FILE")"
assert_contains "$post_action_output" "- [Running] agent-task:45 Choose retry strategy"
assert_contains "$post_action_output" "Mock operator requested resume."
assert_contains "$post_action_output" "sanitized-note Mock resume action -> mock-action:resume"

printf 'Mock runtime smoke passed.\n'
