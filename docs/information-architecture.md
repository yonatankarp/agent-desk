# Agent Desk Information Architecture

Agent Desk is a local-first supervisor console for delegated AI work. Its
interface should help an operator answer four questions without reading raw
logs:

- what happened
- what needs attention
- what evidence supports that state
- what remains not done

This document defines the shared product vocabulary for docs, UI copy, tests,
and future issues. It builds on the milestone completion criteria and the first
read-only desktop replay surface.

## Primary Surfaces

### Replay Status

Replay status explains how the current operator state was derived and how to
interpret it. It names whether the state is sample, loaded, loading, blocked,
or errored. It must distinguish empty queue, not-done, successful outcome, and
Discovery/no-issue triage from product completion.

Replay status is read-only. It does not approve actions, claim live runtime
control, or mark the product complete.

### Work State

Work state lists the current derived work items and their status. It is the
operator's compact answer to "what is active, blocked, terminal, or waiting?"

Work state rows may be derived from imported observations or sample data. They
are not raw imported observations, and they should not expose private source
payloads.

### Timeline

The timeline is the chronological view of accepted, public-safe events. It
shows event type, work item, source, timestamp, detail text, and evidence
summary when available.

Timeline entries are read-only facts derived from sanitized input. The timeline
may show successful outcomes, failures, canceled work, blocked work, and
decision requests, but it must not imply that a successful event means milestone
completion.

### Decision Queue

The decision queue lists work that needs operator attention. In the first
desktop slice, this is derived from blocked and needs-decision work state. Later
slices may add structured decision requests and action proposals.

The queue must not encourage unsafe bulk approval. Disabled, unavailable, or
future action affordances should explain the missing prerequisite instead of
pretending the console can act.

### Evidence Detail

Evidence detail surfaces let the operator inspect why a work item, event,
decision, or status appears. They should show public-safe evidence references,
related timeline entries, and projection warnings when available.

Evidence is a reference to inspectable material, not a place to store raw
private runtime data. Missing evidence should be shown as missing, not inferred.

## Canonical Section Order

Desktop and mobile display structure uses this canonical section order:

| Order | Canonical section | Desktop label today | Mobile label today | Notes |
| --- | --- | --- | --- | --- |
| 1 | Replay status | Replay status | Projection warnings | Mobile currently exposes only the warning/status subset. |
| 2 | Work state | Work state | Current work | Same current-work concept; desktop label is canonical. |
| 3 | Timeline | Read-only timeline | Timeline | `Timeline` is canonical; surfaces render it from the shared display structure. |
| 4 | Decision queue | Decision queue | Attention queue | Same operator-attention concept; desktop label is canonical. |
| 5 | Evidence detail | Evidence drilldown | No standalone label / timeline inline detail | `Evidence detail` is canonical; drilldown/inline expansion are surface-specific presentations. |

Per-surface labels are allowed only when this table records the mapping and
rationale. Unmapped label drift should be treated as a UI structure/copy defect.

### Sources

Sources describe where public-safe observations or events came from, such as a
sample source, sanitized observation file, mock runtime source, or local event
store.

Source labels are provenance hints. They are not credentials, private account
identifiers, chat ids, thread ids, filesystem paths, or runtime session ids.

### Sessions And Agents

Sessions and agents identify the delegated worker context at an operator-safe
level. The first milestone may use work item ids and source labels instead of a
full session/agent directory.

Future session/agent surfaces should show provenance, freshness, and owner
context without importing private runtime internals.

### Reports

Reports summarize slice outcomes, readiness, gaps, verification evidence, and
Discovery output. They must distinguish "no tracked work remains" from "product
complete."

Reports should cite public-safe evidence: issues, PRs, commits, checks, smoke
output, sanitized fixtures, screenshots, and owner/date manual verification.

### Settings And Admin

Settings/admin covers local configuration, source selection, event store
location, public-safety boundaries, and future policy choices.

Settings should be boring and explicit. They should not hide credentials,
remote sync, live runtime control, or outbound actions behind vague toggles.

## Shared Vocabulary

- Observation: sanitized adapter input that can become an event. Observations
  are accepted only after public-safe validation.
- Event: canonical Agent Desk record stored in the local event store and used
  for projection, replay, timeline, and inspection.
- Work item: one bounded unit of delegated work, such as `agent-task:211`.
- Session: a grouped run or worker context. Session identity must be
  public-safe before it appears in Agent Desk.
- Decision: a human choice requested by the system or derived from work state.
- Action proposal: a future structured request to perform an action. A proposal
  is not execution and must remain previewable before approval.
- Evidence: public-safe reference that supports a claim, event, decision, or
  status.
- Source: public-safe origin label for imported or generated state.
- Replay: deterministic derivation of operator state from stored public-safe
  events.
- Completion: a derived product state backed by milestone criteria and
  evidence. It is not a field imported from an adapter.
- Not done: required milestone state or evidence is missing, unknown, blocked,
  or awaiting a decision.

## Read-Only Versus Actionable

Imported observations, event stores, replay status, work state, timeline, and
evidence are read-only in the current product spine. They explain what Agent
Desk knows and why.

Actionable work begins only when a future slice introduces an action proposal,
preview, approval decision, audit record, and a scoped executor. Until then,
buttons or copy must not imply that Agent Desk can stop, resume, approve, post,
send, delete, purchase, or change accounts.

## Desktop And Mobile Boundaries

Desktop is the first dense operator surface. It owns dense scanning, replay
status, work state, timeline, decision queue, evidence detail, and later safe
approval flows.

Desktop and mobile display functionality should stay aligned in both
directions. Mobile may use compact presentation, but it should not omit display
capabilities merely because it is mobile. The intentional divergence is
side-effecting controls: mobile and desktop must not expose stop, resume, retry,
approve, external send, or other side-effect actions until a scoped action
proposal, approval, audit, and executor path exists.

## Copy Guidance

Use precise state language:

- say "read-only replay" instead of "live control"
- say "needs operator attention" instead of "approved automatically"
- say "empty queue" or "no tracked work remains" instead of "complete"
- say "successful outcome" for a completed slice or event, not product
  completion
- say "Discovery/no-issue output is triage" when explaining backlog state
- say "not done" when required evidence or milestone state is missing

Avoid unsafe autonomy:

- do not imply Agent Desk can execute outbound actions without a scoped action
  proposal and approval
- do not present agent interpretation as fact without evidence
- do not echo private paths, credentials, raw transcripts, channel ids, account
  ids, or runtime session ids
- do not hide uncertainty behind green status text

## Non-Goals

This vocabulary does not add live sync, remote/private ingestion, provider
normalization, full dashboards, analytics, account settings, RBAC, outbound
actions, or mobile action execution. Those require separate scoped issues.
