#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP_PARENT="${TMPDIR:-/tmp}"
SMOKE_DIR="$(mktemp -d "${TMP_PARENT%/}/agent-desk-live-inspect.XXXXXX")"

cleanup() {
  rm -rf "$SMOKE_DIR"
}
trap cleanup EXIT

EVENT_STORE="$SMOKE_DIR/events.ndjson"
AUDIT_STORE="$SMOKE_DIR/audit.ndjson"

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

import_output="$(run_cli import-mock-runtime --event-store "$EVENT_STORE")"
assert_contains "$import_output" "Imported 6 mock runtime event(s); skipped 0 duplicate event(s)."

denied_exit=0
denied_output="$(run_cli live-inspect-smoke agent-task:45 --event-store "$EVENT_STORE" --audit-store "$AUDIT_STORE" 2>&1)" || denied_exit=$?
if [[ "$denied_exit" -eq 0 ]]; then
  printf 'Expected unapproved live inspect smoke to exit non-zero (policy denied).\n' >&2
  exit 1
fi
assert_contains "$denied_output" "Live inspect proposal"
assert_contains "$denied_output" "- Denied"
assert_contains "$denied_output" "Inspect proposal denied by operator."
assert_contains "$(cat "$AUDIT_STORE")" '"action":"live-inspect.approval.denied"'

approved_output="$(run_cli live-inspect-smoke agent-task:45 --event-store "$EVENT_STORE" --audit-store "$AUDIT_STORE" --approve)"
assert_contains "$approved_output" "Live inspect proposal"
assert_contains "$approved_output" "- Approved"
assert_contains "$approved_output" "Synthetic inspect completed for agent-task:45 on host:primary."
assert_contains "$approved_output" "live-inspect.adapter.succeeded"
assert_contains "$(cat "$AUDIT_STORE")" '"action":"live-inspect.output.rendered"'

printf 'Live inspect smoke passed: synthetic adapter exercised approval-gated inspect for host=host:primary.\n'
