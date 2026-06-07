# Action Permission Gates

Agent Desk action planning must fail closed before any external or destructive
operation can be connected to a real adapter. Permission gates classify an
operator action, decide the required gate behavior, and emit a compact
public-safe decision log.

## Action Classes

- read-only: inspect or summarize already-sanitized state
- local write: local proposal, mock resume, or local-only note/update
- external send: message, email, webhook, provider write, or outbound API call
- public post: public social, issue, PR, or site publication action
- destructive: stop, delete, archive, revoke, or irreversible mutation
- account/security: auth, permission, billing, security, or identity change
- purchase/payment: purchase, subscription, transfer, or paid action
- credential: token, key, password, cookie, or secret handling action

## Gate Behavior

Read-only actions may be allowed without approval when the proposal is enabled
and public-safe. Local writes require local confirmation. External sends, public
posts, destructive actions, account/security changes, purchase/payment actions,
and credential actions require explicit approval and still remain planning-only
until a future adapter deliberately implements execution.

Unsupported or disabled proposals are denied even when approval is present.
Ambiguous requests require clarification and allow no action. Permission
decisions retain class, behavior, state, actor, target, action, receipt, and a
compact public-safe summary. The audit projection maps each decision — including
denials, cancellations, clarification requests, and unsupported outcomes — to an
audit entry carrying actor, target, action, result, receipt, and the public-safe
summary as detail; gate behavior is available on the decision object, not as a
separate audit column. `AuditTrailRecorder` appends those entries to the durable
local audit store described in [Local audit store](local-audit-store.md), so the
trail is reconstructable after process exit. Each entry carries its own
`recordedAt` timestamp alongside the decision or action time; an entry never
silently reuses decision time as action time. Logs and persisted records do not
store private content.

Non-goals for this slice: live external sends, executor wiring, account changes,
destructive controls, credential storage, or provider-specific permission APIs.
