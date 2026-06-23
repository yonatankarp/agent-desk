#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAR_PATH="$ROOT_DIR/cli/build/libs/agent-desk-cli-all.jar"

cd "$ROOT_DIR"

./gradlew -q :cli:executableJar

dashboard_output="$("${JAVA:-java}" -jar "$JAR_PATH")"
grep -F "Agent Desk" <<<"$dashboard_output" >/dev/null
grep -F "Current work" <<<"$dashboard_output" >/dev/null
grep -F "Attention queue" <<<"$dashboard_output" >/dev/null

host_output="$("${JAVA:-java}" -jar "$JAR_PATH" host-smoke-lab)"
grep -F "Host reachability: host=host:lab state=reachable." <<<"$host_output" >/dev/null
grep -F "state=unreachable failure=network-unavailable" <<<"$host_output" >/dev/null
grep -F "state=timed-out failure=timeout" <<<"$host_output" >/dev/null
grep -F "state=rejected failure=authentication-rejected" <<<"$host_output" >/dev/null
grep -F "state=unsafe-private-detail-redacted failure=unsafe-private-detail-redacted private-detail=redacted" <<<"$host_output" >/dev/null
grep -F "Host connectivity lab passed." <<<"$host_output" >/dev/null

printf 'Runnable operator build smoke passed: cli dashboard and host connectivity diagnostic are available in %s.\n' \
  "cli/build/libs/agent-desk-cli-all.jar"
