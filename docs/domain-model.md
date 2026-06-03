# Domain Model

Agent Desk core types are public-safe and adapter-neutral. They describe supervised work without naming any private runtime, chat system, or local path.

## Package Layout

The `:core` module keeps domain concepts under `com.yonatankarp.agentdesk.core.domain`:

- `domain.entities`: durable domain entities such as `WorkItem`.
- `domain.valueobjects`: identifiers, text values, and lifecycle values such as `WorkItemId`, `WorkItemTitle`, `WorkSummary`, and `WorkStatus`.
- `domain.events`: storage-independent event envelopes and payloads such as `WorkEvent`, `WorkEventId`, `EventSource`, and `WorkStartedPayload`.

The `:app` module keeps reusable application and presentation behavior under `com.yonatankarp.agentdesk.app`:

- `app.operator`: adapter-neutral operator state, projections, sample state, event lines, and status presentations shared by CLI and desktop.

CLI input/output and Compose UI mapping stay adapter-specific. OpenClaw-specific runtime details still belong behind adapters before they reach these shared packages.

Runtime observation imports are described in [Runtime adapter boundary](runtime-adapter-boundary.md).

## Work Item

A work item is one bounded unit of delegated agent work that can be inspected, resumed, stopped, or reviewed.

- `WorkItemId`: stable sanitized identifier, normalized to lowercase.
- `WorkItemTitle`: short human-readable label.
- `WorkSummary`: optional sanitized operational summary.
- `WorkStatus`: durable lifecycle state.

Example:

```text
id: agent-task:42
title: Run public hygiene check
summary: CI failed on the core test task.
status: Blocked
```

## Status Terms

- `Queued`: work is accepted but has not started.
- `Running`: work is actively executing.
- `Waiting`: work is paused on time, retry, or another non-human dependency.
- `NeedsDecision`: work needs an explicit human choice before continuing.
- `Blocked`: work cannot continue without missing access, setup, data, or a resolved failure.
- `Succeeded`: work completed successfully.
- `Failed`: work ended unsuccessfully.
- `Canceled`: work was intentionally stopped before completion.

Terminal statuses are `Succeeded`, `Failed`, and `Canceled`; they do not transition back to active states. `NeedsDecision` and `Blocked` are the only initial statuses treated as requiring human attention.

`Stale` is not a lifecycle status. It should be derived from event timestamps, heartbeats, or runtime health checks so the durable lifecycle state remains stable.

## Event Envelope

`WorkEvent` is the first storage-independent event envelope for deriving current operational state.

- `id`: stable sanitized event identifier.
- `occurredAt`: RFC 3339 UTC instant string.
- `source`: adapter-neutral source identifier, such as `mock-adapter` or `local-daemon`.
- `workItemId`: the work item the event describes.
- `type`: stable event type derived from the payload.
- `payload`: typed event-specific data.

OpenClaw-specific runtime details belong in an adapter before they reach this model. The core event source should stay public-safe and adapter-neutral.

### Public-Safe Examples

Work started:

```text
id: event:agent-task:42:started
occurredAt: 2026-06-02T21:00:00Z
source: mock-adapter
workItemId: agent-task:42
type: work.started
payload.title: Run public hygiene check
payload.summary: Agent accepted the task and started local checks.
```

Work blocked:

```text
id: event:agent-task:42:blocked
occurredAt: 2026-06-02T21:05:00Z
source: mock-adapter
workItemId: agent-task:42
type: work.blocked
payload.reason: CI failed on the core test task.
```

These are illustrative public-safe examples. The stable JSON wire contract is described below.

## Event Serialization

`WorkEventJson` encodes canonical events as compact JSON records. The contract uses stable string fields and `WorkEventType.wireName` values; it does not expose Kotlin class names.

Started event:

```json
{"id":"event:agent-task:42:started","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.started","payload":{"title":"Run public hygiene check","summary":"Agent accepted the task and started local checks."}}
```

Blocked event:

```json
{"id":"event:agent-task:42:blocked","occurredAt":"2026-06-02T21:05:00.123Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.blocked","payload":{"reason":"CI failed on the core test task."}}
```

Record fields:

- `id`: canonical `WorkEventId`.
- `occurredAt`: RFC 3339 UTC timestamp.
- `source`: adapter-neutral `EventSource`.
- `workItemId`: canonical `WorkItemId`.
- `type`: stable event wire name such as `work.started` or `work.blocked`.
- `payload`: event-specific sanitized text fields. Current fields are `title`, `summary`, and `reason`.

The JSON contract must stay public-safe. It must not include local paths, credentials, channel ids, raw transcripts, private runtime ids, or OpenClaw-specific internal fields.

## Event Projection

`WorkEventProjector` derives current operator state from an ordered stream of canonical `WorkEvent` records.

- `WorkStartedPayload` creates or resumes a running `WorkItem` with the event title and summary.
- `WorkNeedsDecisionPayload` and `WorkBlockedPayload` move existing work into human-attention states.
- `WorkSucceededPayload`, `WorkFailedPayload`, and `WorkCanceledPayload` move existing work into terminal states.
- Duplicate event ids are ignored after their first accepted occurrence.
- Events that require missing prior state, or that would leave a terminal state, are ignored with a deterministic `ProjectionIssue`.

The projector applies events in the order supplied by the caller. Storage and adapter code are responsible for choosing a stable ordering before projection; core does not infer ordering from private runtime behavior.
