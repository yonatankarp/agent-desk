#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

./gradlew -q :cli:executableJar

OUTPUT="$("${JAVA:-java}" -jar cli/build/libs/agent-desk-cli-all.jar host-smoke-lab)"

grep -F "Host reachability: host=host:lab state=reachable." <<<"$OUTPUT" >/dev/null
grep -F "state=unreachable failure=network-unavailable" <<<"$OUTPUT" >/dev/null
grep -F "state=timed-out failure=timeout" <<<"$OUTPUT" >/dev/null
grep -F "state=rejected failure=authentication-rejected" <<<"$OUTPUT" >/dev/null
grep -F "state=unsafe-private-detail-redacted failure=unsafe-private-detail-redacted private-detail=redacted" <<<"$OUTPUT" >/dev/null
grep -F "Host connectivity lab passed." <<<"$OUTPUT" >/dev/null

printf '%s\n' "$OUTPUT"
