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
- Update docs when behavior, architecture, or operational workflow changes.
- Add or update tests when code behavior changes.
- Link issues and include evidence in issue comments or PR descriptions.
- Follow the shared engineering style in [docs/engineering-style.md](docs/engineering-style.md).

## Pull Request Expectations

Every PR should state:

- what changed
- why it matters
- checks run
- required roles covered
- any follow-up or blocker
