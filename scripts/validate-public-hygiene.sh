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

  printf 'private path: /home/yonatan/.openclaw/workspace/secret\n' >"$tmpdir/docs/leak.md"
  git -C "$tmpdir" add .

  if AGENT_DESK_HYGIENE_ROOT="$tmpdir" "$script_dir/validate-public-hygiene.sh" >"$tmpdir/negative.out" 2>&1; then
    echo "Expected negative hygiene fixture to fail." >&2
    cat "$tmpdir/negative.out" >&2
    return 1
  fi

  rm "$tmpdir/docs/leak.md"
  printf 'private path: /home/alex/.config/agent-desk/private.properties\n' >"$tmpdir/docs/leak-alternation.md"
  git -C "$tmpdir" add -A

  if AGENT_DESK_HYGIENE_ROOT="$tmpdir" "$script_dir/validate-public-hygiene.sh" >"$tmpdir/alternation.out" 2>&1; then
    echo "Expected non-owner Linux home path to be detected." >&2
    cat "$tmpdir/alternation.out" >&2
    return 1
  fi

  rm "$tmpdir/docs/leak-alternation.md"
  printf 'private path: /Users/jane/Documents/agent-desk/private.log\n' >"$tmpdir/docs/leak-macos.md"
  git -C "$tmpdir" add -A

  if AGENT_DESK_HYGIENE_ROOT="$tmpdir" "$script_dir/validate-public-hygiene.sh" >"$tmpdir/macos.out" 2>&1; then
    echo "Expected macOS user document path to be detected." >&2
    cat "$tmpdir/macos.out" >&2
    return 1
  fi

  rm "$tmpdir/docs/leak-macos.md"
  printf 'private path: C:\\Users\\jane\\.ssh\\id_ed25519\n' >"$tmpdir/docs/leak-windows.md"
  git -C "$tmpdir" add -A

  if AGENT_DESK_HYGIENE_ROOT="$tmpdir" "$script_dir/validate-public-hygiene.sh" >"$tmpdir/windows.out" 2>&1; then
    echo "Expected Windows user config path to be detected." >&2
    cat "$tmpdir/windows.out" >&2
    return 1
  fi

  rm "$tmpdir/docs/leak-windows.md"
  printf 'Policy examples here are intentionally allowlisted: AUTH_TOKEN=example\n' >"$tmpdir/docs/public-hygiene.md"
  git -C "$tmpdir" add -A

  AGENT_DESK_HYGIENE_ROOT="$tmpdir" "$script_dir/validate-public-hygiene.sh" >"$tmpdir/positive.out"
  echo "Public hygiene self-test passed."
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
  ":(exclude)docs/public-hygiene.md"
)

blocked_checks=(
  "private absolute path|/home/yonatan/|/home/[A-Za-z0-9._-]+/\\.(openclaw|ssh|config)|/home/[A-Za-z0-9._-]+/(Desktop|Documents|Downloads|Workspace|workspace)/|/Users/[A-Za-z0-9._-]+/\\.(openclaw|ssh|config)|/Users/[A-Za-z0-9._-]+/(Desktop|Documents|Downloads|Library)/|[A-Za-z]:\\\\Users\\\\[A-Za-z0-9._-]+\\\\(\\.(openclaw|ssh|config)|Desktop|Documents|Downloads|AppData)\\\\"
  "webhook url|(https?://)?(discord(app)?\\.com|hooks\\.slack\\.com)/api/webhooks/[A-Za-z0-9_./-]+"
  "raw token assignment|(^|[^A-Z0-9_])(AUTH_TOKEN|CT0|OPENAI_API_KEY|ANTHROPIC_API_KEY|SLACK_BOT_TOKEN|DISCORD_TOKEN|GITHUB_TOKEN)=[^ .<][^[:space:]]{3,}"
  "private key block|BEGIN (OPENSSH|RSA|EC|DSA) PRIVATE KEY"
  "common token format|gh[pousr]_[A-Za-z0-9_]{20,}|sk-[A-Za-z0-9]{20,}|xox[baprs]-[A-Za-z0-9-]{20,}"
  "raw transcript marker|\\[message_id:[^]]+\\]|^<(system|developer|user|assistant)>$|^### (System|Developer|User|Assistant)$"
  "OpenClaw workspace artifact|\\.openclaw/(sessions|state|workspace)|OpenClaw Workspace Instructions|openclaw\\.inbound_meta"
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
