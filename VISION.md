# Vision

Agent Desk is a supervisor console for delegated AI work.

It is not a chat replacement, a generic dashboard, or a project-management clone. It is an operational surface for answering:

- What is currently running?
- What changed since the last review?
- What needs a human decision?
- What is blocked or stale?
- What did an agent actually do?
- Which actions are safe to approve, stop, resume, or inspect?

## Product Principles

- Evidence before claims: visible state should link to logs, commits, checks, screenshots, traces, or other inspectable artifacts.
- Local-first operation: private integrations stay local and configurable.
- Public-safe repository: no tracked private operational data.
- Adapter boundaries: OpenClaw and other runtimes feed Agent Desk through explicit integration surfaces.
- Shared domain semantics: status, blockers, decisions, and safety states should mean the same thing across desktop, mobile, and backend.

## First Milestone

Prove the smallest useful product loop:

1. Define canonical work/event/status models.
2. Store events and derive current operational state.
3. Show a desktop timeline and decision queue.
4. Exercise one safe action loop against a mock or local adapter.
5. Keep CI, docs, and issue state clean enough for daily autonomous work.

