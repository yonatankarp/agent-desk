# Branch Protection

The deploy key used by the daily agent can read and write repository contents, but it cannot administer GitHub branch protection. A repository owner must configure protection once through GitHub settings or an authenticated admin token.

## Recommended Rules

Protect `main` with:

- Require a pull request before merging.
- Require status checks to pass before merging.
- Require branch to be up to date before merging.
- Required status checks:
  - `Repo Hygiene`
  - `Formatting`
  - `Gradle Build`
  - `Coverage`
  - `Coverage Comment` for same-repo pull requests
- Keep required checks aligned with the current CI workflow as new product surfaces are added.
- Require Code Owner review.
- Restrict force pushes.
- Restrict deletions.
- Require conversation resolution before merging.
- Include administrators if that matches the owner workflow.

This repository includes `.github/CODEOWNERS` with `@yonatankarp` as owner.

Do not require `dependabot`; it is intentionally skipped unless the actor is `dependabot[bot]`. Treat CodeRabbit as review evidence, but do not make it a required branch-protection context unless the repository owner is comfortable with external review rate limits blocking merges.

## Current Verification Status

As of 2026-06-04, the daily agent can verify current CI job names from workflow runs, but cannot read the active `main` branch protection settings. The GitHub branch-protection API call:

```bash
gh api repos/yonatankarp/agent-desk/branches/main/protection
```

returned HTTP 403:

```text
Resource not accessible by personal access token
```

Required follow-up: a repository owner, or a token with repository administration access to read and manage branch protection/rulesets, must verify whether `main` currently enforces the recommended rules above. Until that verification happens, treat branch-protection status as unknown rather than confirmed.

## Admin Helper

An owner can apply the recommended settings with:

```bash
GITHUB_TOKEN=... bash scripts/apply-branch-protection.example.sh
```

The token must have repository administration permission. Do not commit or paste the token into issues, logs, or chat.

## Daily Agent Expectations

Until protection is enabled, the agent may push directly to `main` for bootstrap work if explicitly allowed.

After protection is enabled, the agent should work through branches named `ororo/<short-slice-name>` and use PRs.
