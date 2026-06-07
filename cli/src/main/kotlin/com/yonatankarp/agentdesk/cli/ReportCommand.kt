package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.app.operator.WorkItemInspector
import com.yonatankarp.agentdesk.app.operator.audit.AuditEntry
import com.yonatankarp.agentdesk.app.operator.audit.AuditTrailProjector
import com.yonatankarp.agentdesk.app.operator.verification.CompletionEvidenceChecklist
import com.yonatankarp.agentdesk.app.operator.verification.CompletionEvidenceProjector
import com.yonatankarp.agentdesk.app.operator.verification.CompletionOutcome
import com.yonatankarp.agentdesk.app.operator.verification.CompletionReadinessLabels
import com.yonatankarp.agentdesk.app.persistence.AuditRecordReadResult
import com.yonatankarp.agentdesk.app.persistence.AuditStoreException
import com.yonatankarp.agentdesk.app.persistence.LocalFileAuditRecordRepository
import com.yonatankarp.agentdesk.cli.input.CliInputException
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId

/**
 * The operator-reachable read surface: renders the readiness/verification
 * projection for a work item and reads back the durable audit trail written
 * by act. Read-only: it never mutates the event store or the audit store.
 */
internal object ReportCommand {
    data class Result(
        val text: String,
        val warning: String?,
    )

    fun execute(
        workItemId: WorkItemId,
        events: List<WorkEvent>,
        auditStorePath: String?,
    ): Result {
        val inspection = WorkItemInspector.inspect(events, workItemId)
            ?: throw CliInputException("Work item was not found.")
        val auditRead = auditStorePath?.let(::readAuditStore)
        val trail = auditRead?.entries.orEmpty().filter { entry ->
            entry.target == workItemId || entry.sourceItem == workItemId
        }

        val text = buildString {
            appendLine("Agent Desk")
            appendLine()
            appendLine("Work item")
            appendLine("- $workItemId (${inspection.statusPresentation.label})")
            appendLine()
            appendReadiness()
            appendLine()
            appendAuditTrail(trail, auditStoreConfigured = auditRead != null)
        }.trimEnd()

        return Result(
            text = text,
            warning = auditRead?.trailingCorruption?.publicSafeMessage(),
        )
    }

    private fun StringBuilder.appendReadiness() {
        val readiness = CompletionEvidenceProjector.readiness(unboundChecklist)
        appendLine("Readiness")
        appendLine("- ${CompletionReadinessLabels.labelFor(readiness.state)}")
        readiness.reasons.forEach { reason -> appendLine("- $reason") }
        appendLine()
        appendLine("Verification")
        appendLine("- none")
    }

    private fun StringBuilder.appendAuditTrail(
        trail: List<AuditEntry>,
        auditStoreConfigured: Boolean,
    ) {
        if (!auditStoreConfigured) {
            appendLine("Audit trail")
            appendLine("- No audit store configured. Pass --audit-store <file> to read recorded decisions.")
            return
        }
        appendLine("Audit trail (${trail.size} durable record(s))")
        if (trail.isEmpty()) {
            appendLine("- No durable audit records for this work item.")
            return
        }
        trail.groupBy(AuditEntry::correlationId).forEach { (correlationId, entries) ->
            appendLine("- $correlationId")
            AuditTrailProjector.timelineLines(entries).forEach { line ->
                appendLine("  - ${line.timestamp} ${line.actor} ${line.action} ${line.target} ${line.result}")
            }
        }
    }

    private fun readAuditStore(path: String): AuditRecordReadResult = try {
        LocalFileAuditRecordRepository(parseStorePath(path, label = "audit")).readAll()
    } catch (_: SecurityException) {
        throw CliInputException("Configured audit store could not be read.")
    } catch (exception: AuditStoreException) {
        throw CliInputException(exception.message ?: "Configured audit store could not be read.")
    }

    /**
     * No production path binds verification evidence to the event store yet
     * (owned by #268), so readiness honestly reports the unknown/unverified
     * projection instead of fabricating a checklist source.
     */
    private val unboundChecklist = CompletionEvidenceChecklist(
        outcome = CompletionOutcome.Unknown,
        verificationAttempted = false,
        knownFailures = emptyList(),
        touchedArtifacts = emptyList(),
        residualRisks = emptyList(),
        verificationResults = emptyList(),
    )
}
