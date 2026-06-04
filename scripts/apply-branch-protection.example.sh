#!/usr/bin/env bash
set -euo pipefail

# Requires a GitHub token with repository administration permission.
# Usage:
#   GITHUB_TOKEN=... bash scripts/apply-branch-protection.example.sh

: "${GITHUB_TOKEN:?Set GITHUB_TOKEN with repository administration permission.}"

owner="yonatankarp"
repo="agent-desk"
branch="main"

curl -fsS -X PUT \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer ${GITHUB_TOKEN}" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "https://api.github.com/repos/${owner}/${repo}/branches/${branch}/protection" \
  -d '{
    "required_status_checks": {
      "strict": true,
      "contexts": ["Repo Hygiene", "Formatting", "Gradle Build", "Coverage", "Coverage Comment"]
    },
    "enforce_admins": true,
    "required_pull_request_reviews": {
      "dismiss_stale_reviews": true,
      "require_code_owner_reviews": true,
      "required_approving_review_count": 1,
      "require_last_push_approval": false
    },
    "restrictions": null,
    "required_linear_history": false,
    "allow_force_pushes": false,
    "allow_deletions": false,
    "block_creations": false,
    "required_conversation_resolution": true,
    "lock_branch": false,
    "allow_fork_syncing": true
  }'

echo
echo "Branch protection requested for ${owner}/${repo}:${branch}."
