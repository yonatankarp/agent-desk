---
name: slice-loop
description: "Run Agent Desk issue slices end-to-end with Codex/OpenClaw role checks."
---

# Slice Loop

Use when asked to run the Agent Desk autonomous loop, work the backlog, process issues, or run a timeboxed issue-processing block.

This is the Codex/OpenClaw wrapper for the repo's Claude-oriented loop assets. Claude files are source material, not executable runtime.

Binding policy wins in this order:

1. `AGENTS.md`
2. `docs/roles.md`
3. `.claude/skills/slice-loop/SKILL.md`
4. `.claude/agents/*.md`
5. this wrapper

## Startup

1. Work from the Agent Desk repo root.
2. Fetch latest `origin/main`; if `main` is checked out in another worktree, use a clean worktree or detached `origin/main` safely.
3. Read `AGENTS.md`, `docs/roles.md`, and `.claude/skills/slice-loop/SKILL.md`.
4. Read only the `.claude/agents/<role>.md` files needed for the selected slice.
5. Record stale/dirty worktrees and ignore closed-issue work unless Yonatan explicitly asked for a rerun.

## Runtime Mapping

Claude `subagent_type` names do not exist in Codex. Map them this way:

- For read-only role analysis, spawn bounded Codex/OpenClaw subagents when allowed and useful, passing the relevant `.claude/agents/<role>.md` content or path in the prompt.
- If subagent spawning is unavailable or not worth the overhead, run the role check directly, but label it as a direct role check in the report.
- Never claim a named Claude role agent ran unless it actually did in a compatible runtime.
- The round cap is max 3 discussion/revision rounds, not max 3 agents or checks.

## Role Files

Use these role contracts as prompt templates:

- Manager: `.claude/agents/manager.md`
- Architect: `.claude/agents/architect.md`
- Developer: `.claude/agents/developer.md`
- QA/Tester: `.claude/agents/qa.md`
- Compliance: `.claude/agents/compliance.md`
- Security: `.claude/agents/security.md`
- Designer: `.claude/agents/designer.md`
- Reviewer: `.claude/agents/reviewer.md`
- Discovery: `.claude/agents/discovery.md`
- Docs/Operator: `.claude/agents/docs-operator.md`

Compliance and Security `[blocking]` objections are vetoes. Resolve them or stop; do not tie-break them.

## Loop

Follow `.claude/skills/slice-loop/SKILL.md` for sequencing:

1. Select one actionable issue.
2. Discuss with required roles.
3. Implement in a narrow branch.
4. Judge the diff with QA and Compliance always, plus Security/Reviewer/Designer/others as relevant.
5. Simplify.
6. Open or update the PR.
7. Address comments.
8. Merge only when repo policy permits.
9. Run Discovery and report.
10. Continue only while budget remains and no stop condition applies.

## Continuation Threshold

A tiny cleanup is not a full daily run. After a small slice, run Discovery and then continue to Manager selection when at least 45 minutes remain before cutoff and another actionable slice exists.

Stop early only for a real stop condition: no actionable work after Discovery/no-issue triage, owner decision needed, credentials/external setup, unsafe/destructive action, failed checks requiring judgment, unresolved Security/Compliance veto, stuck PR, or cutoff reached.

## Issue Hygiene

An issue judged stale, irrelevant, duplicate, already satisfied, or overtaken by merged work is not a stop condition by itself. Resolve it as issue hygiene, then continue selection when budget remains.

Required handling:

- If the issue can be closed from public repo evidence, comment with concise evidence, close it, run Discovery, and pick the next actionable issue when time remains.
- If the issue cannot be closed without owner/maintainer input, comment with the exact missing decision and then continue to a different actionable issue unless the whole backlog is blocked.
- Do not report "blocked" merely because the selected issue was a poor pick. Either close/comment it and move on, or prove that all remaining candidates are blocked.
- A no-code run still needs visible movement: issue closed, issue updated with evidence, PR reviewed/merged, new issue created, or a clear all-backlog blocker. Otherwise treat the run as failed, not done.

## Public-Safe Rules

Agent Desk is public. Do not put private OpenClaw paths, real Discord channel IDs, tokens, raw transcripts, personal data, or private logs in tracked files, public issues, PRs, CI output, or screenshots.

Local `.openclaw/` transcripts and local specs/plans are working artifacts. Do not commit them and do not delete them as cleanup.

## Reports

Reports go to the configured Agent Desk channel only. Use `docs/daily-report-template.md` as the exact alert shape.

Hard requirements:

- Start with `🧪 Agent Desk daily report - YYYY-MM-DD`.
- Keep the section order from `docs/daily-report-template.md`.
- Include `🤝 Role checks` and list what actually ran for each applicable role.
- Include `🔁 Revision rounds` separately from role checks.
- Include `🔐 Security/public-safety`, `🧾 Compliance`, `🔍 Evidence`, and `🧭 Discovery`.
- End with `➡️ Next` and `🙋 Needs Yonatan`.
- Do not send a separate "completed and reported" wrapper. The alert itself is the report.
- Keep the report in one Discord message when it fits. If Discord length forces splitting, preserve the section order and do not add a separate status summary.

Do not write `Agent rounds: 0/3` when what you mean is that no role agents ran. Say the direct role checks actually performed.
