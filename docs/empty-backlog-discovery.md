# Empty Backlog Discovery Checklist

An empty actionable issue queue is not evidence that Agent Desk is complete.
When no unblocked `slice` or approved `decision` issue is available, the daily
loop must produce a public-safe discovery result before stopping.

## Required Audit Inputs

Review these inputs and cite the public-safe evidence in the daily report:

- `VISION.md`: unfinished product principles, milestone scope, and non-goals
- open and recently closed GitHub issues
- recent commits on `main`
- current CI state
- docs drift against implemented behavior
- roadmap or milestone gaps in `docs/`
- repeated operator pain from public-safe issue or PR evidence

Do not use stale local worktrees, private notes, raw transcripts, or private
runtime logs as public proof. If private context motivates a gap, translate it
into a public-safe issue with sanitized rationale.

## Required Output

The no-actionable-work report must include one of:

- `created #...`: a new issue with Goal, Acceptance Criteria, Verification,
  and Notes
- `proposed #...`: a decision/proposal issue when product direction is needed
- `none warranted`: evidence reviewed and no implementable public-safe gap was
  found
- `blocked`: the exact public-safe stop reason, such as owner decision needed,
  credentials/setup missing, unsafe action, or failed checks needing judgment

The report must not say or imply that issue exhaustion means product
completion. Use wording like `no actionable issue remained after discovery`,
not `backlog complete`, unless the milestone completion criteria have also been
verified.

## Minimum Report Shape

For the `🧭 Discovery` section, include:

- `Post-merge audit`: not applicable because there was no merged or reviewed
  slice, or the latest slice evidence when the run just completed one
- `Codebase scan`: what public files, TODO/FIXME search, docs, and recent
  commits were checked
- `Follow-up issues`: issues created/proposed, or why none were warranted
- `Manager/Product no-issue triage`: evidence reviewed and stop/creation
  result

For `🙋 Needs Yonatan`, use `Nothing` only when the audit found no owner-facing
decision, setup, or safety blocker.

## Lightweight Local Checklist

Before reporting no actionable work, run or record:

```bash
gh issue list --state open --limit 50
git log --oneline -10 origin/main
rg -n "TODO|FIXME" .
bash scripts/validate-public-hygiene.sh
```

Use equivalent commands when GitHub CLI or local tooling is unavailable, but
record the substitution in the report.
