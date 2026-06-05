#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"

./gradlew -q :desktop:run --args='--smoke-exit'
./gradlew -q :mobile:run --args='--smoke-exit'

printf 'Compose run smoke passed.\n'
