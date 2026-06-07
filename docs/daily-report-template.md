# Daily Report Template

```md
[Agent Desk] Daily report - YYYY-MM-DD

Status: shipped | blocked | no meaningful work | failed checks
Focus: issue or slice

Selection:
- Open backlog reviewed: yes | no
- Closed/stale workspace guard: no stale workspace used | ignored stale workspace for #... | not applicable
- Decision issue handling: selected #... | none open | deferred because ...
- Timebox handling: single-slice run | continued to next slice #... | stopped before expiry because ... | exhausted timebox

Changed:
- ...

Evidence:
- ...

Compliance:
- Architecture boundary check: dependency direction, package/module placement, forbidden imports, and issue acceptance criteria checked | not applicable because ...

Discovery:
- Post-merge audit: acceptance criteria, final PR diff, tests, CI, docs, and user-facing behavior checked | not run because there was no merged or reviewed slice
- Closed-issue acceptance check: verified against merged code/docs with cited evidence | gap found in #... | not applicable because ...
- Operator wiki freshness: checked because operator-facing surface/docs changed | not needed because no operator-facing surface/docs changed | gap found in ...
- Audit skipped reason (allowed: no merged or reviewed slice): ...
- CodeRabbit/review findings: reviewed | unavailable/rate-limited/skipped, continued with local and GitHub evidence
- Codebase scan: boundaries, TODO/FIXME, recently touched areas, public APIs, docs drift, and thin/skipped tests checked | not applicable because ...
- Follow-up issues: created #... | none warranted because ...
- Manager/Product no-issue triage: created/proposed #... | not run because open issue #... selected | none warranted because ...

Issues:
- ...

Next:
- ...

Needs Yonatan:
- Nothing
```
