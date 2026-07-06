# Live Host Action Approval Proposal

Status: Accepted for issue [#389](https://github.com/yonatankarp/agent-desk/issues/389).

This decision defines the first live-host action approval flow. It was accepted
when the owner merged [#408](https://github.com/yonatankarp/agent-desk/pull/408).

## Decision

Agent Desk should implement only one live-host action in the first action
milestone: approval-gated inspect. The action-capable host mode may propose and
run an inspect request against a configured host after explicit operator
approval. Stop, resume, retry, cancel, delete, archive, provider sends,
credential changes, account changes, purchases, and any other mutating live
operation remain out of scope.

The first implementation should treat inspect as a live action even though it is
read-only. It may expose fresher host state than the local event store, so it
must use the same approval and audit discipline that later higher-risk actions
will need.

## In Scope

- propose one inspect action from current operator state
- allow targets for a public-safe work item alias or sanitized evidence alias
- require an accepted host auth state and `action-capable` host permission mode
- require explicit approval of the exact proposal before the host adapter is
  called
- record public-safe audit evidence for proposal, approval or denial, adapter
  result, sanitized output rendering, expiration, and unsafe-output rejection
- reject unsafe proposal targets and unsafe adapter output before rendering
- provide a synthetic adapter or fixture path for tests and smoke checks

## Out Of Scope

- automatic execution without an approval step
- blanket approval for future proposals
- direct use of private runtime ids as public targets
- hashing private ids into public artifacts by default
- raw transcript, prompt, tool input, tool output, path, endpoint, credential,
  process, channel, message, account, or participant rendering
- stop, resume, retry, cancel, or any other mutating live-host action
- provider-native rollback or undo behavior
- external sends, public posts, account/security changes, purchases, and
  credential handling

## Proposal Shape

An inspect proposal should be a public-safe value object created before any host
adapter action. It should contain only:

- proposal id
- correlation id shared by proposal, approval, adapter, and render records
- action kind, initially `inspect`
- host alias, such as `host:primary`
- target kind, such as `work-item` or `evidence`
- public target alias
- compact public-safe summary
- creation time
- expiration time or single-use state
- required operation, `inspect-action-proposal`

Private runtime ids, endpoints, bridge paths, credentials, and raw payload
locations stay in local-only adapter mapping. If the adapter cannot resolve the
public alias locally, the proposal fails with a public-safe unsupported or
not-found result.

## Approval Flow

The approval flow should fail closed:

1. Build a proposal from already-sanitized operator state.
2. Validate that the proposal target and summary pass public-safety checks.
3. Verify the host access boundary allows `inspect-action-proposal`.
4. Persist or emit an audit record for the proposed action.
5. Require explicit approval for that exact proposal id, target alias, action,
   and host alias.
6. On denial, expiration, disabled host mode, auth failure, or target mismatch,
   record the outcome and do not call the host adapter.
7. On approval, call the inspect adapter with local-only target resolution.
8. Validate the adapter response before creating a domain event, audit record,
   or rendered output.
9. Record success, adapter failure, or unsafe-output rejection with the shared
   correlation id.

Approval is one-shot. A proposal must not remain executable after expiration, a
permission-mode downgrade, auth state change, denial, or successful execution.

## Audit Evidence

The audit trail should be reconstructable from local records without reading
private host state. For each proposal, the implementation should record:

- `proposal.created`
- `approval.approved`, `approval.denied`, `approval.expired`, or
  `approval.unavailable`
- `adapter.started` only after approval
- `adapter.succeeded`, `adapter.failed`, or `adapter.unsafe-rejected`
- `output.rendered` when sanitized inspect output is shown to the operator

Records should use existing audit-store discipline: stable public-safe ids,
canonical timestamps, line-oriented local persistence where applicable, no raw
input echoing in errors, and duplicate/corrupt record handling that exposes only
failure class and line number.

## Disablement And Recovery

The fastest rollback for live inspect should be configuration-based:

- set host permission mode to `read-only-observation`, `diagnostic-only`, or
  `unsupported`
- expire existing proposals when the host boundary no longer allows
  `inspect-action-proposal`
- keep audit records readable even when new live actions are disabled
- leave local alias mappings local-only

Because the first action is read-only inspect, there is no provider rollback.
Recovery means disabling new execution, preserving audit evidence, and fixing
the adapter, mapping, or public-safety validation before enabling proposals
again.

## Implementation Notes For Follow-Up

The follow-up implementation issue should cite this proposal after approval and
should keep dependency direction unchanged:

```text
concrete host adapter -> :app runtime/action port -> :core domain model
```

Shared proposal, approval, and audit orchestration should live outside concrete
CLI or desktop rendering code. Concrete adapters resolve local-only targets and
perform the host call. CLI, desktop, and mobile surfaces may render only
public-safe proposal and audit state.

## Verification Expectations

The first implementation should include tests for:

- proposed inspect action from sanitized operator state
- approval required before any adapter call
- denial, expiration, disabled mode, auth failure, and target mismatch
- adapter success, adapter failure, and unsafe output rejection
- audit records grouped by correlation id
- public rendering that excludes private target details and raw runtime content
- synthetic smoke that does not require a real host, endpoint, credential, or
  private runtime export

Public evidence should use sanitized fixture output, test names, check names,
and issue or PR links only.
