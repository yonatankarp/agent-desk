# Dependabot Policy

Dependabot is allowed to keep low-risk tooling dependencies current, but auto-merge must stay behind meaningful CI gates.

## Update Sources

This repository tracks:

- GitHub Actions updates for workflows and actions in `.github/`.
- Gradle updates for the root Kotlin/KMP build.

Dependabot runs weekly on Monday morning in the `Europe/Berlin` timezone.

## Auto-Merge Eligible

Dependabot PRs may auto-merge when all of these are true:

- The PR is opened by `dependabot[bot]`.
- The update is patch or minor.
- The update is for GitHub Actions, Gradle plugins, or Gradle libraries already used by the repository.
- Required checks are green: `Repo Hygiene`, `Formatting`, `Gradle Build`, `macOS Compose Build`, `Windows Compose Build`, and `Coverage`.
- The PR does not change source code, scripts with side effects, repository permissions, or workflow permissions beyond the dependency version bump.

## Review Required

Human review is required for:

- Major version updates.
- Runtime, compiler, Gradle wrapper, Kotlin, KSP, Kotest, Kover, or Konsist updates that change test/build behavior materially.
- Any update that changes workflow permissions, secrets usage, publishing behavior, or authentication.
- Any update that fails CI, flakes repeatedly, or requires source-code changes.
- Any update affecting public-safety checks, branch protection, or Dependabot auto-merge behavior itself.

## Shared Workflow

The CI workflow uses `yonatankarp/github-actions/.github/workflows/dependabot-auto-merge.yml@v2` for Dependabot auto-merge attempts.

Keep that shared workflow only while it remains compatible with this repository's checks and branch-protection rules. If Agent Desk needs repository-specific policy logic, replace it with a local workflow rather than weakening the gates.

## Current Gate Decision

Coverage enforces conservative module-level line thresholds and publishes Kover artifacts. The required `Coverage` check fails if a module drops below its threshold or if a coverage report cannot be generated. Same-repo pull requests also receive a parsed coverage summary comment, but `Coverage Comment` is not a universal required status context because fork pull requests skip the write-token comment job.
