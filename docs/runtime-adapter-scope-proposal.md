# Runtime Adapter Scope Proposal

Status: Proposed for issue [#124](https://github.com/yonatankarp/agent-desk/issues/124). Do not treat this as an approved implementation scope until the issue or pull request has explicit owner approval.

## Proposed Decision

The first concrete non-mock runtime adapter should be an OpenClaw sanitized observation-file adapter.

The adapter is local-only and observation-only. It reads an operator-provided sanitized export file, maps each record into the existing `:app` runtime boundary, and then emits canonical `WorkEvent` records through the current importer path. It must not read private runtime databases, raw chat logs, raw transcripts, local process state, or private screenshots.

No stop, resume, retry, cancel, or approval action loop is included in this first adapter.

## Smallest Emitted Fields

Each accepted observation may emit only:

- `eventId`: generated public-safe event id, not a raw runtime or session id
- `occurredAt`: UTC timestamp
- `source`: adapter-neutral source label such as `openclaw-local`
- `workItemId`: operator-safe alias such as `agent-task:<n>`, not a channel, message, session, node, or process id
- `kind`: one of `Started`, `NeedsDecision`, `Blocked`, `Succeeded`, `Failed`, or `Canceled`
- `title`, `summary`, or `reason`: single-line sanitized text within the existing value-object limits
- evidence references only when already public-safe, using existing evidence kinds such as `commit`, `check-run`, `artifact`, `screenshot`, or `sanitized-note`

## Private Details That Stay Local

The adapter must strip, reject, or translate these before data reaches `RuntimeWorkObservation` or `WorkEvent`:

- raw transcripts, prompts, tool inputs and outputs, private logs, and private screenshots
- Discord channel ids, message ids, OpenClaw session ids, node ids, process ids, socket paths, and local host paths
- tokens, credentials, private URLs, personal data, and workspace-specific paths
- exact raw runtime ids unless an operator-provided alias map translates them first

Stable aliases should come from explicit local alias mapping. Do not hash private ids into public artifacts as the default. If a field cannot be made public-safe under existing value-object validation, the adapter should reject or drop it instead of weakening domain checks.

## Verification Evidence

The first implementation slice should provide public-safe evidence only:

- unit tests mapping sanitized OpenClaw-like export records into `RuntimeWorkObservation` and `WorkEvent`
- a checked-in sanitized fixture export with no private paths, raw transcripts, channel ids, or runtime internals
- a local smoke command that imports the fixture into a temporary event store and renders operator state
- PR evidence based on test output and sanitized fixture snippets, not private logs, screenshots, or transcripts

## Follow-Up Gate

Implementation issues should be created only after this proposal is explicitly approved or replaced. If the owner chooses not to build a concrete adapter yet, record that decision in `docs/decision-log.md` instead.
