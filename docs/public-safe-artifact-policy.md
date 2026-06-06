# Public-Safe Artifact Policy And Operator Runbooks

Agent Desk artifacts are useful only when they can be inspected, committed, and
shared without exposing private runtime context. Treat every log, fixture,
screenshot, report, diagnostic, and replay archive as public by default unless a
policy explicitly says it must stay local.

## Artifact Classes

| Artifact | Safe To Commit/Share | Redaction Required | Retention Expectation |
| --- | --- | --- | --- |
| Logs | compact public-safe summaries, check names, task names, counts | raw paths, tokens, raw transcripts, private ids, private URLs | keep only issue/PR-relevant snippets |
| Screenshots | UI screenshots with no private content and no live account data | private messages, account names, tokens, file paths, browser bars with private URLs | keep only current review evidence |
| Fixtures | synthetic public-safe ids, relative docs paths, sanitized note targets | real runtime ids, real user data, credentials, copied transcripts | keep with tests when synthetic |
| Runtime observations | canonical work/event ids, adapter labels, public-safe summaries | raw source payloads, private source ids, internal session markers | keep canonical events; drop raw source |
| Reports | status, evidence links, checks, risks, public issue/PR references | private rationale, raw tool output, private URLs, channel/message ids | keep release/readiness reports |
| Generated docs | architecture decisions, runbooks, matrices, public-safe examples | secrets, private logs, raw chats, private screenshots | keep when linked from docs index |
| Debug dumps | generally not publishable | everything private by default | local-only, delete after diagnosis |
| Replay archives | canonical public-safe events and diagnostics | raw importer input, private logs, local absolute paths | keep only sanitized replay archives |

## GitHub Issue And PR Content

Acceptable:

- public issue, PR, commit, workflow, and check names
- exact local commands that do not include private paths or secrets
- public-safe summaries of failures and diagnostics
- relative repository paths
- synthetic fixtures used by tests

Unacceptable:

- secrets, tokens, passwords, cookies, deploy keys, or credential aliases
- raw transcripts, private chat excerpts, private channel/message ids, or runtime session ids
- private file paths, private URLs, local browser URLs, or account dashboards
- screenshots containing private messages, names, credentials, or private browser chrome
- raw tool output that includes private payloads

When in doubt, summarize the failure class and link to sanitized evidence rather
than pasting raw output.

## Verification Commands

Use these before publishing or merging artifact-related work:

```bash
bash scripts/validate-public-hygiene.sh
git diff --check
./gradlew spotlessCheck
./gradlew :app:allTests :app:build
```

For docs-only slices, public hygiene, diff check, and Spotless are the minimum
local gates. For code that touches runtime, persistence, UI, or reports, run the
focused test plus the strongest relevant module gate.

## Runbooks

### Startup

1. Pull latest `main`.
2. Check `git status --short --branch`.
3. Confirm no other agent or user is writing to the same worktree.
4. Create a branch or clean worktree before editing.
5. Read the relevant issue and nearby docs before changing files.

### Smoke Testing

1. Run the focused command for the touched surface.
2. Run `bash scripts/validate-public-hygiene.sh`.
3. Run `git diff --check`.
4. Run `./gradlew spotlessCheck`.
5. For app/runtime changes, run `./gradlew :app:allTests :app:build`.
6. Record exact commands in the PR body.

### Failed Observations

1. Keep raw source input local.
2. Inspect mapper/importer diagnostics for the rejected boundary.
3. Add or update a synthetic fixture if the failure class is not covered.
4. Report only sanitized diagnostic kind, count, and public-safe summary.
5. Do not paste rejected payloads into issues or PRs.

### Failed Tool Calls

1. Record the command or tool name and public-safe failure class.
2. Redact raw paths, tokens, private ids, and private URLs from any summary.
3. Rerun only after checking the command cannot mutate external state unexpectedly.
4. If the failure blocks completion, update the issue/PR with next safe step.

### Corrupted Local State

1. Stop writing to the affected store.
2. Copy no raw records into GitHub.
3. Use public-safe diagnostics such as corrupt line number or duplicate event id.
4. Rebuild from sanitized replay events when available.
5. If local-only state must be discarded, ask before destructive cleanup.

### Failed Release

1. Freeze new release actions.
2. Collect public CI check names, commit ids, PR links, and sanitized failure summaries.
3. Update the readiness checklist with failed gates and residual risks.
4. Reopen or create a bounded follow-up issue for the failed gate.
5. Do not publish artifacts until required gates are green or explicitly waived.

### Suspected Leak

1. Stop sharing the artifact immediately.
2. Identify the artifact class, boundary, and public surface where it appeared.
3. Remove or rotate exposed credentials through the appropriate account owner.
4. Replace the artifact with a sanitized summary.
5. Add a negative regression test for the leak class.
6. Document only sanitized incident facts in the repo.

### Accidental External Action

1. Stop further external actions.
2. Capture public-safe receipt metadata: action class, target class, time, and resulting state.
3. Notify the operator before attempting remediation.
4. Prefer reversible provider-native undo paths when approved.
5. Add or tighten permission-gate coverage before resuming automation.

## What Not To Paste Or Share

- raw private logs or transcripts
- raw runtime context
- private screenshots
- local absolute paths
- cookies, tokens, passwords, one-time codes, or key material
- private account, billing, or security pages
- raw message, thread, session, or channel identifiers
- unsanitized tool call inputs or outputs

This policy is a public-safe operating baseline, not comprehensive compliance,
DLP, incident response, or provider-specific retention policy.
