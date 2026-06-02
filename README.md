# Agent Desk

Agent Desk is a public-safe, local-first supervisor console for delegated AI work.

The product goal is simple: open the app after time away, understand what agents are doing, see what needs a decision, and drill into evidence quickly.

This repository is Kotlin/KMP-first:

- Kotlin shared core for domain models, event schemas, reducers, and sync/state logic.
- Compose Multiplatform desktop as the primary client target.
- iOS support through a native shell backed by shared Kotlin logic, unless product evidence later favors Compose iOS.
- A backend or local daemon can be Kotlin/Ktor when needed.
- OpenClaw is treated as one integration source behind an adapter boundary.

## Current Status

The repository is in bootstrap mode. The first slice establishes public-safe repo hygiene, CI, Dependabot, role expectations, and branch-protection guidance before product code lands.

## Public-Safe Rule

Assume every commit, issue, CI log, screenshot, and report is public.

Do not commit private paths, tokens, real channel IDs, raw agent transcripts, personal data, private URLs, or OpenClaw-internal secrets. Use adapters, templates, and sanitized examples for local integrations.

