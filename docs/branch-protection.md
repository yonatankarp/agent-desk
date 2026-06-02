# Branch Protection

The deploy key used by the daily agent can read and write repository contents, but it cannot administer GitHub branch protection. A repository owner must configure protection once through GitHub settings or an authenticated admin token.

## Recommended Rules

Protect `main` with:

- Require a pull request before merging.
- Require status checks to pass before merging.
- Require branch to be up to date before merging.
- Required status check: `Repo Hygiene`.
- Add required shared workflow checks as they are adopted from `yonatankarp/github-actions`.
- Require Code Owner review.
- Restrict force pushes.
- Restrict deletions.
- Require conversation resolution before merging.
- Include administrators if that matches the owner workflow.

This repository includes `.github/CODEOWNERS` with `@yonatankarp` as owner.

## Admin Helper

An owner can apply the recommended settings with:

```bash
GITHUB_TOKEN=... bash scripts/apply-branch-protection.example.sh
```

The token must have repository administration permission. Do not commit or paste the token into issues, logs, or chat.

## Daily Agent Expectations

Until protection is enabled, the agent may push directly to `main` for bootstrap work if explicitly allowed.

After protection is enabled, the agent should work through branches named `ororo/<short-slice-name>` and use PRs.
