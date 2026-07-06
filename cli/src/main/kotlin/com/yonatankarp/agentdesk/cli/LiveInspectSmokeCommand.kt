package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.app.operator.Actor
import com.yonatankarp.agentdesk.app.operator.action.LiveHostInspectAction
import com.yonatankarp.agentdesk.app.operator.action.LiveHostInspectApproval
import com.yonatankarp.agentdesk.app.operator.action.LiveHostInspectOutcome
import com.yonatankarp.agentdesk.app.operator.action.LiveHostInspectProposal
import com.yonatankarp.agentdesk.app.operator.action.SyntheticLiveHostInspectAdapter
import com.yonatankarp.agentdesk.app.operator.audit.AuditDisplayLabels
import com.yonatankarp.agentdesk.app.operator.audit.AuditEntry
import com.yonatankarp.agentdesk.app.operator.audit.AuditTrailProjector
import com.yonatankarp.agentdesk.app.operator.audit.AuditTrailRecorder
import com.yonatankarp.agentdesk.app.persistence.AuditStoreException
import com.yonatankarp.agentdesk.app.persistence.LocalFileAuditRecordRepository
import com.yonatankarp.agentdesk.app.persistence.LocalFileWorkEventRepository
import com.yonatankarp.agentdesk.app.persistence.WorkEventStoreException
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostAccessBoundary
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostAlias
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostAuthState
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostPermissionMode
import com.yonatankarp.agentdesk.cli.input.CliInputException
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId

internal object LiveInspectSmokeCommand {
    data class Result(
        val text: String,
        val exitCode: Int,
    )

    fun execute(
        workItemId: WorkItemId,
        eventStorePath: String,
        auditStorePath: String,
        approve: Boolean,
        now: EventTimestamp,
    ): Result {
        val hostAlias = RuntimeHostAlias.parse("host:primary")
        val outcome = inspect(
            workItemId = workItemId,
            eventStorePath = eventStorePath,
            auditStorePath = auditStorePath,
            approval = if (approve) exactApproval(workItemId, hostAlias, now) else LiveHostInspectApproval.Deny,
            hostAlias = hostAlias,
            now = now,
        )
        return Result(
            text = render(workItemId, approve, outcome),
            exitCode = if (outcome is LiveHostInspectOutcome.Executed) 0 else POLICY_DENIED_EXIT_CODE,
        )
    }

    private fun inspect(
        workItemId: WorkItemId,
        eventStorePath: String,
        auditStorePath: String,
        approval: LiveHostInspectApproval,
        hostAlias: RuntimeHostAlias,
        now: EventTimestamp,
    ): LiveHostInspectOutcome = try {
        val eventPath = parseStorePath(eventStorePath, label = "event")
        val auditPath = parseStorePath(auditStorePath, label = "audit")
        val outcome = LiveHostInspectAction(
            eventRepository = LocalFileWorkEventRepository(eventPath),
            auditRecorder = AuditTrailRecorder(LocalFileAuditRecordRepository(auditPath)),
            hostBoundary = RuntimeHostAccessBoundary(
                alias = hostAlias,
                authState = RuntimeHostAuthState.Accepted,
                permissionMode = RuntimeHostPermissionMode.ActionCapable,
            ),
            adapter = SyntheticLiveHostInspectAdapter,
            expiresAt = EventTimestamp.parse("2026-12-31T23:59:00Z"),
        ).inspect(
            workItemId = workItemId,
            actor = Actor.parse("operator:cli"),
            approval = approval,
            now = now,
        )
        if (outcome is LiveHostInspectOutcome.WorkItemNotFound) {
            throw CliInputException("Work item was not found.")
        }
        outcome
    } catch (_: SecurityException) {
        throw CliInputException("Configured inspect stores could not be updated.")
    } catch (exception: WorkEventStoreException) {
        throw CliInputException(exception.message ?: "Configured event store could not be read.")
    } catch (exception: AuditStoreException) {
        throw CliInputException(exception.message ?: "Configured audit store could not be updated.")
    }

    private fun render(
        workItemId: WorkItemId,
        approve: Boolean,
        outcome: LiveHostInspectOutcome,
    ): String = buildString {
        appendLine("Agent Desk")
        appendLine()
        appendLine("Live inspect proposal")
        appendLine("- inspect $workItemId on host:primary")
        appendLine("- Synthetic adapter only; no real host action is run.")
        appendLine()
        when (outcome) {
            is LiveHostInspectOutcome.Executed -> {
                appendLine("Outcome")
                appendLine("- Approved")
                appendLine("- ${outcome.output.summary}")
                appendLine()
                appendAuditTrail(outcome.auditEntries)
            }

            is LiveHostInspectOutcome.NotExecuted -> {
                appendLine("Outcome")
                appendLine("- Denied")
                appendLine("- ${outcome.reason}")
                appendLine()
                appendAuditTrail(outcome.auditEntries)
                appendLine()
                appendLine("Next step")
                appendLine("- ${if (approve) "Inspect the audit trail." else "Re-run with --approve to execute the synthetic inspect adapter."}")
            }

            LiveHostInspectOutcome.WorkItemNotFound -> appendLine("Work item was not found.")
        }
    }.trimEnd()

    private fun StringBuilder.appendAuditTrail(entries: List<AuditEntry>) {
        appendLine("Audit trail (${entries.size} durable record(s))")
        AuditTrailProjector.timelineLines(entries).forEach { line ->
            appendLine("- ${line.timestamp} ${line.actor} ${line.action} ${line.target} ${line.result}")
        }
    }

    private fun exactApproval(
        workItemId: WorkItemId,
        hostAlias: RuntimeHostAlias,
        now: EventTimestamp,
    ): LiveHostInspectApproval = LiveHostInspectApproval.Approve(
        proposalId = LiveHostInspectProposal.idFor(workItemId, now),
        hostAlias = hostAlias,
        target = workItemId,
    )

    private const val POLICY_DENIED_EXIT_CODE = 3
}
