---
name: slice-loop
description: Use when asked to run the autonomous issue-processing loop, work the backlog, or process repo issues end-to-end with the role-agent team — e.g. "run the loop", "/slice-loop 2h", "work 3 issues", "process the backlog".
---

# Slice Loop

Drive the issue → discuss → implement → judge → simplify → PR → merge loop with the repo's role team. This skill owns sequencing and mechanics only. Policy lives in the binding docs and wins on any conflict: `AGENTS.md` (run contract, stop conditions, branch policy, Discovery, reports) and `docs/roles.md` (role duties).

Role agents are defined in `.claude/agents/` (`manager`, `architect`, `developer`, `qa`, `compliance`, `security`, `designer`, `reviewer`, `discovery`, `docs-operator`). Spawn each role with its matching subagent type instead of an ad-hoc prompt; the spawn prompt supplies only the phase (discuss/judge), the issue or diff, and the transcript path.

## Budget argument

- `2h` (timebox, per AGENTS.md Timeboxed Runs) | `3` (issue count) | `all` (until backlog empty) | none → one issue.

## Iteration

1. **Select.** Pull latest `main`. Triage per AGENTS.md Daily Run steps 2–3: prefer unblocked `slice` issues; handle `decision` issues per the contract; ignore closed issues and stale worktrees. No actionable issue → Discovery scan per AGENTS.md, report, stop.
2. **Discuss.** Pick required roles and record why. Per round: spawn the role agents in parallel; each gets the issue plus the transcript at `.openclaw/discussions/<issue>.md` (gitignored — never commit, never delete) and returns a position with objections tagged `blocking` or `minor`. Converged = no blocking objections. **Max 3 rounds.** After round 3: orchestrator tie-breaks small disputes (log the tie-break in the transcript); big disputes → label the issue `decision`, comment the options, skip to the next issue. Security and Compliance blocking objections cannot be tie-broken.
3. **Implement.** Main agent implements the agreed design on a fresh branch with the correct release-note prefix (AGENTS.md branch policy). TDD. Full test suite green before proceeding. Scope is frozen at convergence.
4. **Judge.** Relevant roles judge the diff: QA and Compliance always, Designer for UI surfaces. Findings → fix → re-judge, **max 3 rounds**. Leftover minor findings → follow-up issues (see Findings rule). Blocking finding unresolved after round 3 → stop the run; never merge over a veto.
5. **Simplify.** One code-simplifier pass over the diff (reuse, dead code, clarity). Apply safe fixes, re-run the full test suite.
6. **PR.** Open the PR with title and labels per AGENTS.md. Wait for CI and any in-flight review flows.
7. **Address comments.** Every comment fixed or answered with reasoning; threads resolved. A new blocking finding from a human reviewer counts as a judge round toward the cap of 3.
8. **Merge.** Enable auto-merge; merge happens when CI is green and in-flight flows are done.
9. **Post-merge.** Discovery audit, issue closure with evidence, and report per AGENTS.md. Delete branch and worktree. Budget left → next issue, else final loop report.

## CodeRabbit is best-effort

Wait at most ~10 minutes for CodeRabbit to start or finish. If it is rate-limited, skipped, or silent after that, record that explicitly (AGENTS.md requires the note) and proceed to merge on green CI. Do NOT wait indefinitely, and do NOT treat a pending CodeRabbit review as a merge blocker. If it does report, address its comments like any reviewer.

## Stuck PR vs stuck issue (asymmetric on purpose)

- **Stuck issue** (discussion deadlock escalated to `decision`, blocked dependency): skip to the next issue; the run continues.
- **Stuck PR** (CI failing after 3 fix attempts, unresolved blocking veto): **stop the whole run** and report. Do not park the PR and pick another issue — an unmerged slice is unfinished work that the next iteration would conflict with. Never merge around it.

## Findings rule (single writer)

Any role, in any phase, tags out-of-scope findings as `follow-up` instead of arguing for inclusion — the current slice never absorbs them. Manager is the only role that files GitHub issues (Goal, Acceptance Criteria, Verification). Discovery finds; Manager files. Evidence-backed gaps only (AGENTS.md anti-noise guard). Filed issues join the backlog and compete on priority at the next Select — no queue jumping.

## Local artifacts

`.openclaw/` transcripts and `docs/superpowers/` specs/plans are local working artifacts: never commit them, and do not delete them as "cleanup" — they are gitignored and stay on disk. This repo is public; nothing private (OpenClaw paths, channel IDs, tokens, transcripts, personal data) goes into tracked files, issues, PRs, or CI output.

## Stop conditions

Any AGENTS.md stop condition (credentials, external setup, product direction, destructive operations, unguessable decisions) halts the run with a report. Do not keep working merely to consume the budget.

## Red flags — stop and re-read this skill

- "Security's objection is overly cautious, CI is green" → veto holds; stop the run.
- "It's only a 20-line unrelated fix" → follow-up issue, not scope creep.
- "CodeRabbit is still pending, better wait" → bounded wait, then record and proceed.
- "CI keeps failing, I'll grab another issue meanwhile" → stuck PR stops the run.
- "This local spec/transcript should be committed (or deleted)" → neither; it stays local.
- "One more discussion round will settle it" → 3 is the cap; tie-break or escalate.