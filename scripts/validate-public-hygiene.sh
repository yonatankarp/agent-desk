#!/usr/bin/env bash
set -euo pipefail

script_path="${BASH_SOURCE[0]}"
script_dir="$(cd "$(dirname "$script_path")" && pwd)"
repo_root="${AGENT_DESK_HYGIENE_ROOT:-$(pwd)}"

run_self_test() {
  local tmpdir
  tmpdir="$(mktemp -d)"
  trap 'rm -rf "$tmpdir"' RETURN

  git -C "$tmpdir" init -q
  mkdir -p \
    "$tmpdir/.github/workflows" \
    "$tmpdir/docs" \
    "$tmpdir/scripts"

  local required_file
  for required_file in "${required_files[@]}"; do
    mkdir -p "$tmpdir/$(dirname "$required_file")"
    printf 'public-safe fixture\n' >"$tmpdir/$required_file"
  done

  write_fixture "$tmpdir/docs/leak-linux-owner.md" 'private path: %s%s/%s%s\n' "/home/" "yonatan" ".openclaw" "/workspace/secret"
  expect_hygiene_failure "linux-owner"
  rm "$tmpdir/docs/leak-linux-owner.md"

  write_fixture "$tmpdir/docs/leak-linux-generic.md" 'private path: %s%s%s\n' "/home/" "alex" "/projects/agent-desk/private.log"
  expect_hygiene_failure "linux-generic"
  rm "$tmpdir/docs/leak-linux-generic.md"

  write_fixture "$tmpdir/docs/leak-linux-hidden.md" 'private path: %s%s%s\n' "/home/" "alex" "/.config/agent-desk/private.properties"
  expect_hygiene_failure "linux-hidden"
  rm "$tmpdir/docs/leak-linux-hidden.md"

  write_fixture "$tmpdir/docs/leak-macos-generic.md" 'private path: %s%s%s\n' "/Users/" "jane" "/projects/agent-desk/private.log"
  expect_hygiene_failure "macos-generic"
  rm "$tmpdir/docs/leak-macos-generic.md"

  write_fixture "$tmpdir/docs/leak-macos-library.md" 'private path: %s%s%s\n' "/Users/" "jane" "/Library/Application Support/agent-desk/private.log"
  expect_hygiene_failure "macos-library"
  rm "$tmpdir/docs/leak-macos-library.md"

  write_fixture "$tmpdir/docs/leak-windows-generic.md" 'private path: %s%s%s\n' "C:\\Users\\" "jane" "\\source\\repos\\agent-desk\\private.log"
  expect_hygiene_failure "windows-generic"
  rm "$tmpdir/docs/leak-windows-generic.md"

  write_fixture "$tmpdir/docs/leak-windows-drive.md" 'private path: %s%s%s\n' "D:\\Users\\" "jane" "\\projects\\agent-desk\\private.log"
  expect_hygiene_failure "windows-drive"
  rm "$tmpdir/docs/leak-windows-drive.md"

  write_fixture "$tmpdir/docs/leak-windows-appdata.md" 'private path: %s%s%s\n' "C:\\Users\\" "jane" "\\AppData\\Roaming\\agent-desk\\private.log"
  expect_hygiene_failure "windows-appdata"
  rm "$tmpdir/docs/leak-windows-appdata.md"

  write_fixture "$tmpdir/docs/leak-windows-forward.md" 'private path: %s%s%s\n' "C:/Users/" "jane" "/source/agent-desk/private.txt"
  expect_hygiene_failure "windows-forward"
  rm "$tmpdir/docs/leak-windows-forward.md"

  write_fixture "$tmpdir/docs/public-hygiene.md" 'bad token example: %s=%s\n' "AUTH_TOKEN" "example"
  expect_hygiene_failure "public-hygiene-token-assignment"

  write_fixture "$tmpdir/docs/public-hygiene.md" 'bad token example: %s-%s\n' "sk" "aaaaaaaaaaaaaaaaaaaaaaaa"
  expect_hygiene_failure "public-hygiene-openai-token"

  write_fixture "$tmpdir/docs/public-hygiene.md" 'bad token example: %s_%s\n' "ghp" "aaaaaaaaaaaaaaaaaaaaaaaa"
  expect_hygiene_failure "public-hygiene-github-token"

  write_fixture "$tmpdir/docs/public-hygiene.md" 'bad webhook example: https://%s/api/webhooks/%s\n' "discord.com" "123456789012345678/fixture-token"
  expect_hygiene_failure "public-hygiene-webhook"

  write_fixture "$tmpdir/docs/public-hygiene.md" 'private path example: %s%s%s\n' "/home/" "alex" "/agent-desk/private.log"
  expect_hygiene_failure "public-hygiene-private-path"

  {
    printf 'placeholder Linux: /home/<user>/agent-desk/events.ndjson\n'
    printf 'placeholder macOS: /Users/<user>/agent-desk/events.ndjson\n'
    printf 'placeholder Windows: C:\\Users\\<user>\\agent-desk\\events.ndjson\n'
    printf 'placeholder home env: ${HOME}/agent-desk-events.ndjson\n'
    printf 'placeholder Windows env: %%USERPROFILE%%\\agent-desk-events.ndjson\n'
  } >"$tmpdir/docs/placeholder-paths.md"
  {
    printf 'safe token placeholder: AUTH_TOKEN=<redacted>\n'
    printf 'safe key placeholder: OPENAI_API_KEY=<set in environment>\n'
    printf 'safe webhook placeholder: https://discord.com/api/webhooks/<webhook-id>/<webhook-token>\n'
    printf 'safe GitHub placeholder: ghp_<redacted>\n'
    printf 'safe Slack placeholder: xoxb-<redacted>\n'
  } >"$tmpdir/docs/public-hygiene.md"
  git -C "$tmpdir" add -A

  AGENT_DESK_HYGIENE_ROOT="$tmpdir" "$script_dir/validate-public-hygiene.sh" >"$tmpdir/positive.out"
  echo "Public hygiene self-test passed."
}

write_fixture() {
  local path="$1"
  local format="$2"
  shift 2
  printf "$format" "$@" >"$path"
  git -C "$(dirname "$(dirname "$path")")" add -A
}

expect_hygiene_failure() {
  local label="$1"
  if AGENT_DESK_HYGIENE_ROOT="$tmpdir" "$script_dir/validate-public-hygiene.sh" >"$tmpdir/$label.out" 2>&1; then
    echo "Expected hygiene fixture to fail: $label" >&2
    cat "$tmpdir/$label.out" >&2
    return 1
  fi
}

required_files=(
  "README.md"
  "VISION.md"
  "AGENTS.md"
  "CONTRIBUTING.md"
  "scripts/validate-public-hygiene.sh"
  "docs/roles.md"
  "docs/branch-protection.md"
  "docs/decision-log.md"
  "docs/public-hygiene.md"
  ".github/workflows/ci.yml"
  ".github/dependabot.yml"
)

if [[ "${1:-}" == "--self-test" ]]; then
  run_self_test
  exit 0
fi

cd "$repo_root"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "Public hygiene validation must run inside a git work tree." >&2
  exit 1
fi

for path in "${required_files[@]}"; do
  if [[ ! -f "$path" ]]; then
    echo "Missing required file: $path" >&2
    exit 1
  fi
done

scan_pathspecs=(
  "README.md"
  "VISION.md"
  "AGENTS.md"
  "CONTRIBUTING.md"
  "docs"
  "scripts"
  ".github"
  ":(glob)**/*.kt"
  ":(glob)**/*.kts"
  ":(glob)**/*.toml"
)

allowlisted_pathspecs=(
  ":(exclude)scripts/validate-public-hygiene.sh"
)

openclaw_instructions_pattern="OpenClaw Workspace"
openclaw_instructions_pattern="${openclaw_instructions_pattern} Instructions"
openclaw_meta_pattern="openclaw"
openclaw_meta_pattern="${openclaw_meta_pattern}\\.inbound_meta"
message_marker_pattern="\\[message"
message_marker_pattern="${message_marker_pattern}_id:[^]]+\\]"

blocked_checks=(
  "linux user home path|/home/[A-Za-z0-9._-]+/"
  "macos user home path|/Users/[A-Za-z0-9._-]+/"
  "windows user profile path|[A-Za-z]:[\\\\/]+Users[\\\\/]+[A-Za-z0-9._-]+[\\\\/]+"
  "webhook url|(https?://)?(discord(app)?\\.com|hooks\\.slack\\.com)/api/webhooks/[A-Za-z0-9_./-]+"
  "raw token assignment|(^|[^A-Z0-9_])(AUTH_TOKEN|CT0|OPENAI_API_KEY|ANTHROPIC_API_KEY|SLACK_BOT_TOKEN|DISCORD_TOKEN|GITHUB_TOKEN)=[^ .<][^[:space:]]{3,}"
  "private key block|BEGIN (OPENSSH|RSA|EC|DSA) PRIVATE KEY"
  "common token format|gh[pousr]_[A-Za-z0-9_]{20,}|sk-[A-Za-z0-9]{20,}|xox[baprs]-[A-Za-z0-9-]{20,}"
  "raw transcript marker|${message_marker_pattern}|^<(system|developer|user|assistant)>$|^### (System|Developer|User|Assistant)$"
  "OpenClaw workspace artifact|\\.openclaw/(sessions|state|workspace)|${openclaw_instructions_pattern}|${openclaw_meta_pattern}"
  "real-looking chat or channel id|(chat|channel|guild|message|discord|slack)[_-]?id[\"' :=]+[0-9]{17,20}|(slack|channel)[_-]?id[\"' :=]+C[A-Z0-9]{8,}"
)

for check in "${blocked_checks[@]}"; do
  label="${check%%|*}"
  pattern="${check#*|}"
  if git grep -nIE "$pattern" -- "${scan_pathspecs[@]}" "${allowlisted_pathspecs[@]}"; then
    echo "Blocked public-safety pattern found: $label" >&2
    exit 1
  fi
done

echo "Public hygiene validation passed."
