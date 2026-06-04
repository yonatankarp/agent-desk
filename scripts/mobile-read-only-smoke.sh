#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"

./gradlew -q :mobile:jvmTest --tests 'com.yonatankarp.agentdesk.mobile.MobileSmokeSnapshotTest'

printf 'Mobile read-only smoke passed.\n'
