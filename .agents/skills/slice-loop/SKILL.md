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

## Public-Safe Rules

Agent Desk is public. Do not put private OpenClaw paths, real Discord channel IDs, tokens, raw transcripts, personal data, or private logs in tracked files, public issues, PRs, CI output, or screenshots.

Local `.openclaw/` transcripts and local specs/plans are working artifacts. Do not commit them and do not delete them as cleanup.

## Reports

Reports go to the configured Agent Desk channel only. They must distinguish:

- Role checks actually run
- Revision/discussion rounds used out of 3
- Verification evidence
- Security/public-safety result
- Discovery output or why no follow-up was warranted
- Needs-Yonatan blocker, if any

Do not write `Agent rounds: 0/3` when what you mean is that no role agents ran. Say the direct role checks actually performed.
