# Engineering Style

Agent Desk code should stay small, explicit, and easy to move across adapters. These rules capture the current project style decisions for agents and humans.

## Kotlin Style

- Prefer one public production class, value object, interface, or sealed hierarchy per file.
- Keep tiny private helpers near the code they support.
- Keep tightly coupled sealed hierarchies in one file when splitting would make the model harder to read.
- Prefer `"...".toRegex()` or raw-string regex literals such as `"""\s+""".toRegex()` over direct `Regex("...")` construction unless options or named construction clarify intent.
- Avoid duplicated validation logic across value objects. Use shared internal validators or shared composite value objects when two domain concepts share the same constraints.

## Domain Modeling

- Keep `:core` public-safe and adapter-neutral.
- Model durable business concepts as explicit domain types, not primitive strings passed through adapters.
- Prefer a shared value object underneath semantically similar wrappers when the structure is identical but names differ in the ubiquitous language.
- Add `application`, port, and adapter packages only when a slice introduces real orchestration or integration behavior.
- Do not leak OpenClaw runtime details, private paths, chat IDs, local process IDs, or raw transcripts into domain packages.

## CLI And Desktop Boundaries

- CLI and Compose desktop are adapters over shared application and presentation behavior.
- Do not put reusable application behavior inside CLI-only code.
- Build shared use cases, projections, and presenters in the `:app` module so CLI and desktop can both call them.
- Keep `:core` domain-only; shared operator state and presentation projections belong in `:app`, not `:core`.
- Define runtime import ports and sanitized mappers in `:app`; concrete runtime adapters depend inward on those contracts.
- Keep CLI input, CLI rendering, Compose state mapping, and runtime integration behind separate adapter boundaries as they grow.

## Tests

- Use Kotest assertions in Kotest tests. Do not use JUnit assertions unless a test is specifically exercising JUnit integration.
- Use Kotest `given` / `when` / `then` style for behavior tests where it improves readability.
- Architecture tests such as Konsist may stay rule-oriented instead of forcing given/when/then.
- Avoid duplicated test data. Prefer fixtures and a small test DSL for common domain objects, events, and projections.
- Keep fixtures public-safe and deterministic.

## Documentation

- Keep `README.md` focused on what the project is, how to run it, and where to find deeper material.
- Put architecture decisions, code style, domain details, and longer process notes in `docs/`.
- Keep examples sanitized and public-safe.
