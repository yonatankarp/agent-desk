# Privacy Boundary Regression

Agent Desk treats public-safe output as a product boundary. Tests should reject
private material before it can appear in runtime observations, import
diagnostics, persisted event artifacts, UI summaries, audit lines, or generated
operator reports.

## Safe To Persist, Commit, Or Publish

- canonical work ids such as `agent-task:410`
- canonical event ids such as `event:agent-task:410:blocked`
- public-safe timestamps, statuses, and adapter labels
- sanitized notes and relative documentation paths
- compact diagnostics such as imported, skipped, unsafe-rejected, or store-rejected counts
- public CI check names and public artifact labels
- summaries that describe behavior without raw private data

## Must Be Rejected Or Redacted

- tokens, credentials, passwords, cookies, deploy keys, or secret markers
- private URLs, localhost URLs, private IPs, file URLs, and URLs with userinfo
- raw transcript markers or conversation dumps
- local paths from Linux, macOS, Windows, or shell home shortcuts
- raw channel/message ids and `channel:`, `message:`, `thread:`, or `session:` identifiers
- private runtime/session ids and internal OpenClaw runtime markers
- unsanitized screenshots or logs containing private payloads

Regression tests should assert two things: unsafe input is rejected, and the
error or diagnostic does not echo the private sample payload. These checks are a
representative safety net, not comprehensive DLP, private alias discovery, or a
replacement for manual public-safe review before publishing artifacts.
