# Live Host Connectivity Milestone

Status: Accepted for issue [#378](https://github.com/yonatankarp/agent-desk/issues/378) on 2026-06-19.

## Decision

Agent Desk live-host work is staged into three milestones:

1. **2A: diagnostic-only host connectivity.** Agent Desk can validate local host configuration, map private endpoint details to an operator-safe alias, distinguish reachability and permission failures, and render that status in operator surfaces.
2. **2B: read-only live observation sync.** Agent Desk can import current work observations from a configured host through the adapter boundary, using only public-safe aliases and sanitized evidence.
3. **2C: approval-gated live inspect action.** Agent Desk can propose and, after explicit approval, run a low-risk inspect action through the host adapter and record a public-safe audit trail.

Stop, resume, retry, cancel, and other mutating live-host actions remain out of scope until the live action proposal and approval flow is separately accepted.

## User-Visible Done State

Milestone 2A is done when an operator can answer these questions without reading raw runtime state:

- Is a host configured?
- Which public-safe host alias is being checked?
- Is the host reachable from the local environment?
- If not reachable, is the problem missing configuration, invalid configuration, network reachability, timeout, authentication, unsupported mode, or unsafe input?
- Does the CLI or desktop surface show unreachable host state as a not-done/blocking condition rather than empty or completed work?
- Which public-safe local smoke or lab result proves the current status?

## Public-Safe Evidence

Public artifacts may include:

- issue and PR links
- sanitized host aliases such as `host:primary`
- diagnostic state labels
- sanitized fixture output
- local lab/smoke command names and public-safe summaries
- CI checks and public-safe test names
- docs explaining the local-only configuration pattern

Public artifacts must not include:

- hostnames, IP addresses, ports, private URLs, or socket paths
- tokens, credentials, cookies, or authorization headers
- raw runtime ids, session ids, process ids, channel ids, or message ids
- raw transcripts, prompts, tool inputs or outputs, private logs, or private screenshots
- local filesystem paths or workspace-specific private state

## Diagnostic States

The first diagnostic model should distinguish at least:

- not configured
- invalid configuration
- reachable
- unreachable
- timed out
- authentication missing
- authentication rejected or expired
- unsupported host mode
- unsafe payload rejected

These states should use public-safe text and explicit aliases. Private endpoint details stay in local configuration only.

## Follow-Up Sequence

Recommended implementation order:

1. [#384](https://github.com/yonatankarp/agent-desk/issues/384): define local host profile and alias mapping.
2. [#385](https://github.com/yonatankarp/agent-desk/issues/385): define authentication and permission boundary.
3. [#379](https://github.com/yonatankarp/agent-desk/issues/379): add LAN host reachability diagnostic contract.
4. [#383](https://github.com/yonatankarp/agent-desk/issues/383): implement local host connection smoke command.
5. [#388](https://github.com/yonatankarp/agent-desk/issues/388): add end-to-end local lab scenario.
6. [#382](https://github.com/yonatankarp/agent-desk/issues/382): surface connectivity status in CLI and desktop views.
7. [#381](https://github.com/yonatankarp/agent-desk/issues/381): add the failed local-network host connection runbook.
8. [#390](https://github.com/yonatankarp/agent-desk/issues/390): implement read-only live observation sync.
9. [#389](https://github.com/yonatankarp/agent-desk/issues/389): decide live host action proposal and approval flow.
10. [#387](https://github.com/yonatankarp/agent-desk/issues/387): implement the first approval-gated inspect action.
11. [#386](https://github.com/yonatankarp/agent-desk/issues/386): load configured operator state in mobile.
12. [#391](https://github.com/yonatankarp/agent-desk/issues/391): package a runnable local operator build with connectivity diagnostics.

[#380](https://github.com/yonatankarp/agent-desk/issues/380) can run early in parallel because it prevents issue exhaustion from masking unfinished product scope again.

## Non-Goals

- Direct private runtime database reads.
- Raw chat-log or transcript ingestion.
- Public screenshots containing private runtime data.
- Automatic host control without explicit approval.
- Mutating live actions before the action decision is accepted.
- Treating an empty work queue as product completion when connectivity diagnostics are missing or failing.

## Verification Expectations

Each follow-up slice should include:

- tests for success and failure states relevant to that slice
- public-safe rendering checks for aliases and diagnostic text
- local smoke or lab evidence when touching host connectivity behavior
- docs updates when operator behavior or local setup changes

The private real host may be used for local operator validation, but public PR evidence must rely on sanitized/lab output rather than private endpoint details.

## Local Host Profile Contract

The diagnostic milestone uses an operator-provided host profile with:

- `hostAlias`: public-safe alias shown in diagnostics and operator surfaces
- `hostEndpoint`: local-only endpoint configuration that must never be rendered
  into public artifacts
- `hostAliasMappings`: optional explicit runtime-id to public-alias mappings

Real host profile files are private runtime configuration. Keep them outside the
repository or in ignored `agent-desk.host*.properties` files. The checked-in
template [agent-desk.host.example.properties](agent-desk.host.example.properties)
uses placeholders only.
