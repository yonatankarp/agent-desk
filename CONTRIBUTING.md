# Contributing

Agent Desk is developed in public, so repository hygiene is part of the product.

## Public-Safe Contributions

Do not include:

- tokens, keys, credentials, cookies, or secrets
- real channel IDs or private service IDs
- private local paths
- raw agent transcripts
- screenshots containing personal or private operational data
- private URLs or internal-only links

Use `.example` files, placeholders, sanitized fixtures, and adapter interfaces.

## Changes

- Keep changes small and coherent.
- Name branches with a release-note-aware prefix such as `feat/`, `fix/`, `docs/`, `ci/`, `build/`, `tooling/`, `arch/`, `refactor/`, `chore/`, or `breaking/`.
- Update docs when behavior, architecture, or operational workflow changes.
- Add or update tests when code behavior changes.
- Link issues and include evidence in issue comments or PR descriptions.
- Follow the shared engineering style in [docs/engineering-style.md](docs/engineering-style.md).

## Pull Request Expectations

PR titles should be concise and may use the same intent prefix as the branch, such as `feat:`, `fix:`, `docs:`, `ci:`, `build:`, `refactor:`, or `chore:`.

Every PR should state:

- what changed
- why it matters
- checks run
- required roles covered
- any follow-up or blocker
