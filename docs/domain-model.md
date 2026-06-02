# Domain Model

Agent Desk core types are public-safe and adapter-neutral. They describe supervised work without naming any private runtime, chat system, or local path.

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
