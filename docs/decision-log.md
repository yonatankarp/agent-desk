# Decision Log

## 2026-06-03: Branch-aware PR release labels

Decision: Agent Desk uses branch-prefix and path-based PR auto-labeling to apply release-note labels before releases are generated.

Rationale: Generated release notes are only as useful as the labels on merged PRs. Encoding branch conventions in `AGENTS.md` and `CONTRIBUTING.md` gives agents and contributors a shared source of truth, while path fallback keeps labeling useful when a branch name is generic.

Consequence: Contributors and agents should create branches with prefixes such as `feat/`, `fix/`, `docs/`, `ci/`, `build/`, `tooling/`, `arch/`, `refactor/`, `chore/`, or `breaking/`. The PR labeler adds matching labels but does not remove manually curated labels.

## 2026-06-03: Manual SemVer release workflow

Decision: Agent Desk releases are created by manually running the `Release` GitHub Actions workflow from `main` with an explicit `patch`, `minor`, or `major` SemVer bump.

Rationale: Letting the workflow own version calculation, tag creation, release creation, generated notes, and jar upload avoids UI/tag ordering races and makes release intent explicit.

Consequence: Maintainers should not create release tags or releases manually for normal releases. The workflow uses a focused SemVer increment action for version calculation and keeps tag/release/asset operations explicit through GitHub CLI/API calls.

## 2026-06-03: Label-driven generated release notes

Decision: Agent Desk uses GitHub's generated release notes configuration to group merged PRs by public-safe labels instead of maintaining custom release-note scripts.

Rationale: The release workflow asks GitHub to generate notes. A small `.github/release.yml` keeps the release surface maintainable, reviewable, and aligned with ordinary PR labeling.

Consequence: Maintainers should label PRs before running the release workflow. PRs without a matching category still appear under Other Changes, while explicit skip labels keep noise out of public releases.

## 2026-06-03: Tag-driven CLI jar releases

Decision: Agent Desk releases are created from `v*` Git tags and publish the executable CLI jar as both an Actions artifact and a GitHub Release asset.

Rationale: Tag-driven releases keep the public pipeline minimal, auditable, and free of secrets while making the packaged CLI easy to retrieve.

Consequence: Superseded by the manual SemVer release workflow above after tag-first testing exposed UI/tag ordering failures.

## 2026-06-02: KMP-first product direction

Decision: Agent Desk is Kotlin/KMP-first. Kotlin shared core owns domain models, event schemas, reducers, filtering, and sync/state logic. Desktop and mobile are first-class clients.

Rationale: The product needs consistent operational semantics across desktop, mobile, and backend surfaces. Shared domain logic matters more than matching OpenClaw's current Node-heavy implementation.

Consequence: Node should only appear in adapter code where it naturally touches existing Node-based runtimes. OpenClaw is an integration source, not the architecture center.

## 2026-06-02: Public-safe repository from first commit

Decision: The repository is designed for public visibility from commit one.

Rationale: Public CI is useful and the project may become reusable. Public-safe constraints avoid cleanup later.

Consequence: No tracked private paths, channel IDs, tokens, raw transcripts, private logs, or unsanitized screenshots.

## 2026-06-02: Prefer shared GitHub Actions

Decision: Agent Desk should reuse applicable workflows and composite actions from `yonatankarp/github-actions`.

Rationale: Shared CI keeps repeated repository setup consistent and makes Dependabot/branch-protection expectations easier to maintain.

Consequence: Before adding local workflow logic, inspect the shared actions repository. Defer adopting shared JVM/Gradle workflows until Agent Desk has a real Gradle/KMP project shape for them to run against.

## 2026-06-02: Desktop starts with CLI plus Compose, sequenced separately

Decision: Agent Desk desktop scope includes both a thin CLI operator surface and a Compose Multiplatform graphical shell. Build the CLI slice first, then the Compose desktop shell as a separate slice.

Rationale: CLI support gives an inspectable, automation-friendly operator surface that can exercise the shared core event/status model before graphical layout decisions harden. Compose Multiplatform remains the primary graphical desktop console, but it should build on core semantics rather than invent UI-only state.

Consequence: The first desktop implementation slice is [#26](https://github.com/yonatankarp/agent-desk/issues/26), a public-safe CLI operator surface. The follow-up graphical shell slice is [#27](https://github.com/yonatankarp/agent-desk/issues/27), which requires Designer review and should show minimal mock/sample state from the core model.
