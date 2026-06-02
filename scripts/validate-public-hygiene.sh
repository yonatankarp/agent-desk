#!/usr/bin/env bash
set -euo pipefail

required_files=(
  "README.md"
  "VISION.md"
  "AGENTS.md"
  "CONTRIBUTING.md"
  "docs/roles.md"
  "docs/branch-protection.md"
  "docs/decision-log.md"
  ".github/workflows/ci.yml"
  ".github/dependabot.yml"
)

for path in "${required_files[@]}"; do
  if [[ ! -f "$path" ]]; then
    echo "Missing required file: $path" >&2
    exit 1
  fi
done

blocked_patterns=(
  "discord(app)?\\.com/api/webhooks"
  "AUTH_TOKEN="
  "CT0="
  "BEGIN OPENSSH PRIVATE KEY"
  "BEGIN RSA PRIVATE KEY"
  "xox[baprs]-"
)

for pattern in "${blocked_patterns[@]}"; do
  if grep -RInE "$pattern" --exclude-dir=.git --exclude=validate-public-hygiene.sh .; then
    echo "Blocked public-safety pattern found: $pattern" >&2
    exit 1
  fi
done

echo "Public hygiene validation passed."
