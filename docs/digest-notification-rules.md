# Digest And Notification Rules

Agent Desk classifies accepted operator state into digest-ready attention rules. This is a read-only policy surface: it does not send messages, create provider jobs, configure channels, or persist an outbox. Delivery integration stays deferred until a separate approved slice introduces an adapter boundary.

## Rule Inputs

Rules derive only from `OperatorState`: accepted work items, accepted work events, sanitized evidence references, and derived stale-attention records. They do not inspect raw runtime transcripts, private paths, provider account identifiers, channel identifiers, webhook configuration, or external message metadata.

## Delivery Modes

- `ReadOnly`: all digest signals remain locally inspectable as immediate candidates for the console or report model. No external action is taken.
- `Quiet`: all digest signals remain locally inspectable, but only high or critical urgency signals are immediate candidates. Normal and low urgency items stay in grouped digests.

Both modes suppress outbound delivery. Quiet mode changes attention escalation only; it never hides state from the local operator surface.

## Rules

| Rule | Source | Urgency | Digest group | Operator meaning |
| --- | --- | --- | --- | --- |
| `DecisionRequested` | current work status is `NeedsDecision` | high | pending decisions | Work cannot continue until the operator inspects evidence or defers. |
| `WorkBlocked` | current work status is `Blocked` | high | new blockers | Work is blocked and needs operator attention. |
| `WorkStale` | derived stale-attention record for running or waiting work | normal | material changes | No accepted event arrived within the stale threshold. |
| `WorkFailed` | current work status is `Failed` | critical | new blockers | Work reached a failed terminal state. |
| `WorkCompleted` | current work status is `Succeeded` | low | completed work | A work item reached a successful outcome. This is not product completion. |
| `MaterialChange` | latest accepted event includes public-safe evidence while work remains running or waiting | normal | material changes | New inspectable evidence is available for active work. |

## Deduplication

Each digest signal has a stable public-safe dedupe key:

```text
<rule-key>:<work-item-id>
```

Repeated observations with the same key roll up into one digest item. The representative item keeps the latest accepted timestamp and an occurrence count. Dedupe is model-only in this slice; it is not persisted and does not create a send history.

## Digest Windows And Groups

Digest projection may receive a `DigestWindow` with inclusive start and end timestamps. Signals outside the window stay out of that digest run. The window is a local model filter only; it does not schedule delivery or persist notification state.

Digest groups are optimized for operator scanning:

- `PendingDecisions`: decision requests that still need review.
- `NewBlockers`: blocked or failed work.
- `CompletedWork`: successful work-item outcomes.
- `MaterialChanges`: stale work and evidence-backed changes that did not move the lifecycle into attention or terminal state.

This policy intentionally defines windowing, grouping, and dedupe semantics without adding a scheduler or delivery channel.

## Examples

Decision requested:

- Input: accepted `work.needs-decision` event for `agent-task:42`.
- Output: `DecisionRequested`, high urgency, `PendingDecisions`, dedupe key `decision-requested:agent-task:42`.
- Delivery: inspectable locally; no outbound delivery occurs.

Blocked:

- Input: current status `Blocked`.
- Output: `WorkBlocked`, high urgency, `NewBlockers`.
- Delivery: inspectable locally; no provider action is taken.

Stale:

- Input: running work appears in `staleAttention` with `staleForMinutes = 90`.
- Output: `WorkStale`, normal urgency, `MaterialChanges`.
- Delivery: visible in the digest even in quiet mode, but not an immediate quiet-mode candidate.

Failed:

- Input: current status `Failed`.
- Output: `WorkFailed`, critical urgency, `NewBlockers`.
- Delivery: remains an immediate candidate in quiet mode because it is critical.

Completed:

- Input: current status `Succeeded`.
- Output: `WorkCompleted`, low urgency, `CompletedWork`.
- Delivery: grouped into the digest; it does not claim the product or milestone is complete.

Material change:

- Input: latest accepted event includes a sanitized evidence reference while the work item remains running or waiting.
- Output: `MaterialChange`, normal urgency, `MaterialChanges`.
- Delivery: grouped locally; no outbound delivery occurs.

Dedupe:

- Input: two accepted decision events for the same work item.
- Output: one `DecisionRequested` item with the latest timestamp and occurrence count `2`.
- Delivery: one digest item, not repeated external sends.
