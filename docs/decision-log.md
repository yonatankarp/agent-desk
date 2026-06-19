# Decision Log

## 2026-06-19: Live host connectivity is staged before control

Decision: Live-host work is split into three stages: diagnostic-only host connectivity, read-only live observation sync, and approval-gated live inspect. Mutating live actions such as stop, resume, retry, and cancel remain deferred until the live action proposal and approval flow is separately accepted.

Rationale: The existing runtime adapter proves sanitized offline replay, not actual host reachability. Operators need Agent Desk to show whether a configured host alias is reachable and why it is not, while keeping private endpoint details local-only and out of public artifacts. Jumping directly to live control would mix networking, authentication, public-safety, observation sync, and action approval before the diagnostic foundation is inspectable.

Consequence: Follow-up work should start with local host profile/alias mapping, authentication and permission boundaries, reachability diagnostics, smoke/lab verification, and operator status surfacing. Read-only live sync comes after diagnostics are stable. Approval-gated inspect comes after read-only sync. Mutating actions remain out of scope until explicitly approved. Full scope details live in [Live host connectivity milestone](live-host-connectivity-milestone.md).

## 2026-06-15: Shared Compose design tokens live in `:design`

Decision: Shared Compose design tokens, status colors, typography, spacing, theme helpers, and reusable Compose components live in the existing `:design` Compose Multiplatform module. `:app` remains Compose-free and owns semantic operator state such as `StatusTone`; `:design` maps those semantics into Compose presentation primitives consumed by desktop and mobile.

Rationale: A dedicated Compose module gives desktop and mobile one visual source of truth without pulling Compose dependencies into `:app`, CLI, daemon, or runtime-adapter code. The current build graph already matches this direction: `:design` depends on `:app`, while desktop and mobile depend on `:design`. This preserves dependency direction, keeps client styling aligned, and avoids duplicated shell palettes drifting again.

Consequence: Design-system follow-up work should extend `:design` rather than creating a parallel `:ui` module or moving Compose primitives into `:app`. Issues blocked by the placement decision should be retargeted to the existing `:design` module and closed when their remaining acceptance criteria are already satisfied by merged design-system work.

## 2026-06-05: First runtime adapter scope

Decision: The first concrete non-mock runtime adapter is an OpenClaw sanitized observation-file adapter, not a direct private runtime database/chat-log adapter or a control/action adapter.

Rationale: Agent Desk can validate real runtime integration without importing private transcripts, channel ids, session ids, local paths, process details, screenshots, credentials, or workspace-specific state into public artifacts. A sanitized observation file keeps the first adapter local-only, observation-only, testable with checked-in fixtures, and compatible with the existing `RuntimeWorkObservation` to `WorkEvent` importer boundary.

Smallest workflow: The adapter reads an operator-provided sanitized export file, validates public-safe fields, maps accepted records to `RuntimeWorkObservation`, and lets the existing importer emit canonical `WorkEvent` records. It may represent `Started`, `NeedsDecision`, `Blocked`, `Succeeded`, `Failed`, and `Canceled` lifecycle observations. It does not stop, resume, retry, cancel, approve, or otherwise control runtime work.

Translation rule: Stable aliases should come from explicit local alias mapping. Do not hash private ids into public artifacts by default. If a field cannot satisfy existing value-object validation, reject or drop it instead of weakening public-safe domain checks.

Consequence: The first implementation follow-ups should add the sanitized observation-file adapter, a checked-in sanitized fixture export, importer tests, and a local smoke command that imports the fixture into a temporary event store and renders operator state. Full scope details live in [Runtime adapter scope decision](runtime-adapter-scope-decision.md).

## 2026-06-04: First mobile client scope

Decision: The first mobile proof is a Compose Multiplatform mobile surface backed by shared Kotlin state from `:app`, not a native iOS-only shell, Android-only shell, or deferred mobile shell.

Rationale: Agent Desk is Kotlin/KMP-first, desktop and mobile are first-class targets, and the desktop product already uses Compose Multiplatform for the graphical shell. A Compose mobile proof keeps the first mobile surface aligned with the existing shared UI/runtime direction instead of adding a native client stack before the product shape is stable. Starting read-only keeps the mobile scope small enough to verify without introducing action approval risk.

Smallest workflow: Mobile should first show read-only current work plus the attention queue from sample or stored events. The view should expose work id, title, status presentation, summary or reason text, stale-attention markers, compact evidence references, and projection warnings when present. It should not trigger stop, resume, retry, or approval actions until the read model and screenshot evidence are stable.

Shared-state contract: `:app` should own the mobile-facing read model by deriving it from existing `OperatorState` projections and presenters. CLI rendering, Compose desktop state, Compose mobile views, runtime imports, local file persistence, and OpenClaw-specific observation details remain adapter- or client-specific. Runtime adapters may feed sanitized events into `:app`, but private paths, channel ids, raw transcripts, credentials, and runtime internals must not cross into the mobile contract.

Verification expectations: The first mobile implementation should include `:app` tests for sample and stored-event projections, attention queue ordering, stale attention, evidence references, and projection warnings. Any screenshot or smoke artifact should be public-safe and show only sanitized read-only current work plus attention queue state.

Consequence: The first implementable follow-up is [#116](https://github.com/yonatankarp/agent-desk/issues/116), which adds the shared mobile read-only operator state contract before Compose mobile shell wiring.

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

Decision: Agent Desk releases are created from `v*` Git tags and publish the executable CLI jar as both an Actions artifact and a GitHub Release asset. Release-critical gates run before tag creation: public hygiene, Spotless, CLI tests, mock runtime smoke, executable jar build, and executable jar smoke.

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
