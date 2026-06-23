#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP_PARENT="${TMPDIR:-/tmp}"
SMOKE_DIR="$(mktemp -d "${TMP_PARENT%/}/agent-desk-live-sync.XXXXXX")"

cleanup() {
  rm -rf "$SMOKE_DIR"
}
trap cleanup EXIT

OBSERVATIONS_FILE="$ROOT_DIR/app/src/jvmTest/resources/openclaw/sanitized-observations.json"
HOST_CONFIG="$SMOKE_DIR/agent-desk.host.properties"
EVENT_STORE="$SMOKE_DIR/events.ndjson"
PORT_FILE="$SMOKE_DIR/port"
READY_FILE="$SMOKE_DIR/ready"

python3 - "$PORT_FILE" "$READY_FILE" <<'PY' &
import socket
import sys

port_file, ready_file = sys.argv[1], sys.argv[2]
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind(("127.0.0.1", 0))
server.listen()
with open(port_file, "w", encoding="utf-8") as handle:
    handle.write(str(server.getsockname()[1]))
with open(ready_file, "w", encoding="utf-8") as handle:
    handle.write("ready")
while True:
    connection, _ = server.accept()
    connection.close()
PY
SERVER_PID=$!
trap 'kill "$SERVER_PID" 2>/dev/null || true; cleanup' EXIT

until [[ -f "$READY_FILE" ]]; do
  sleep 0.1
done
HOST_PORT="$(cat "$PORT_FILE")"

cat >"$HOST_CONFIG" <<EOF
hostAlias=host:primary
hostEndpoint=http://127.0.0.1:$HOST_PORT/status
hostAuthState=accepted
hostPermissionMode=read-only-observation
hostObservationBridge=$OBSERVATIONS_FILE
EOF

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

sync_output="$(run_cli sync-live-observations --host-config "$HOST_CONFIG" --event-store "$EVENT_STORE")"
assert_contains "$sync_output" "Live observation sync: host=host:primary state=synced freshness=fresh"
assert_contains "$sync_output" "Imported 10 live observation event(s); skipped 0 duplicate event(s)."
assert_contains "$sync_output" "Diagnostics: imported=10 skipped-duplicate=0 invalid=0 unsafe-rejected=0 store-rejected=0 redacted-or-dropped=0."

duplicate_output="$(run_cli sync-live-observations --host-config "$HOST_CONFIG" --event-store "$EVENT_STORE")"
assert_contains "$duplicate_output" "Imported 0 live observation event(s); skipped 10 duplicate event(s)."
assert_contains "$duplicate_output" "Diagnostics: imported=0 skipped-duplicate=10 invalid=0 unsafe-rejected=0 store-rejected=0 redacted-or-dropped=0."

printf 'Live observation sync smoke passed: read-only bridge imported public-safe observations for host=host:primary.\n'
