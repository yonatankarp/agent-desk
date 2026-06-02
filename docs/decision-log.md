# Decision Log

## 2026-06-02: KMP-first product direction

Decision: Agent Desk is Kotlin/KMP-first. Kotlin shared core owns domain models, event schemas, reducers, filtering, and sync/state logic. Desktop and mobile are first-class clients.

Rationale: The product needs consistent operational semantics across desktop, mobile, and backend surfaces. Shared domain logic matters more than matching OpenClaw's current Node-heavy implementation.

Consequence: Node should only appear in adapter code where it naturally touches existing Node-based runtimes. OpenClaw is an integration source, not the architecture center.

## 2026-06-02: Public-safe repository from first commit

Decision: The repository is designed for public visibility from commit one.

Rationale: Public CI is useful and the project may become reusable. Public-safe constraints avoid cleanup later.

Consequence: No tracked private paths, channel IDs, tokens, raw transcripts, private logs, or unsanitized screenshots.

