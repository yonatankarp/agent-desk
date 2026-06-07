package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.app.config.ConfigValidationException
import com.yonatankarp.agentdesk.app.config.EventStoreLocation
import com.yonatankarp.agentdesk.app.operator.Actor
import com.yonatankarp.agentdesk.app.operator.OperatorActionException
import com.yonatankarp.agentdesk.app.operator.OperatorActionIntent
import com.yonatankarp.agentdesk.app.operator.OperatorStateProjectionException
import com.yonatankarp.agentdesk.app.operator.action.ActOnWorkItem
import com.yonatankarp.agentdesk.app.operator.action.ActOutcome
import com.yonatankarp.agentdesk.app.operator.action.PermissionGateBehavior
import com.yonatankarp.agentdesk.app.operator.audit.AuditEntry
import com.yonatankarp.agentdesk.app.operator.audit.AuditTrailProjector
import com.yonatankarp.agentdesk.app.operator.audit.AuditTrailRecorder
import com.yonatankarp.agentdesk.app.persistence.AuditStoreException
import com.yonatankarp.agentdesk.app.persistence.LocalFileAuditRecordRepository
import com.yonatankarp.agentdesk.app.persistence.LocalFileWorkEventRepository
import com.yonatankarp.agentdesk.app.persistence.WorkEventStoreException
import com.yonatankarp.agentdesk.cli.input.CliInputException
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import java.nio.file.InvalidPathException
import java.nio.file.Path

/**
 * The operator-reachable action path: routes an act invocation through the
 * shared `ActOnWorkItem` use case (planner -> gate -> approval loop -> durable
 * audit recorder) and renders the public-safe outcome sections.
 */
internal object ActCommand {
    data class Result(
        val text: String,
        val exitCode: Int,
    )

    fun execute(
        intent: OperatorActionIntent,
        workItemId: WorkItemId,
        eventStorePath: String,
        auditStorePath: String,
        approve: Boolean,
        now: EventTimestamp,
    ): Result {
        val outcome = act(intent, workItemId, eventStorePath, auditStorePath, approve, now)
        return Result(
            text = render(intent, workItemId, approve, outcome),
            exitCode = if (outcome is ActOutcome.NotExecuted) POLICY_DENIED_EXIT_CODE else 0,
        )
    }

    private fun act(
        intent: OperatorActionIntent,
        workItemId: WorkItemId,
        eventStorePath: String,
        auditStorePath: String,
        approve: Boolean,
        now: EventTimestamp,
    ): ActOutcome = try {
        val eventPath = parseStorePath(eventStorePath, label = "event")
        val auditPath = parseStorePath(auditStorePath, label = "audit")
        val outcome = ActOnWorkItem(
            eventRepository = LocalFileWorkEventRepository(eventPath),
            auditRecorder = AuditTrailRecorder(LocalFileAuditRecordRepository(auditPath)),
        ).act(
            intent = intent,
            workItemId = workItemId,
            actor = cliActor,
            approved = approve,
            now = now,
        )
        if (outcome is ActOutcome.WorkItemNotFound) {
            throw CliInputException("Work item was not found.")
        }
        outcome
    } catch (_: SecurityException) {
        throw CliInputException("Configured action stores could not be updated.")
    } catch (exception: WorkEventStoreException) {
        throw CliInputException(exception.message ?: "Configured event store could not be updated.")
    } catch (exception: AuditStoreException) {
        throw CliInputException(exception.message ?: "Configured audit store could not be updated.")
    } catch (exception: OperatorActionException) {
        throw CliInputException(exception.message ?: "Mock operator action could not be applied.")
    } catch (exception: OperatorStateProjectionException) {
        throw CliInputException(exception.message ?: "Mock operator action could not read operator state.")
    }

    private fun parseStorePath(
        path: String,
        label: String,
    ): Path = try {
        Path.of(EventStoreLocation.parse(path).value)
    } catch (exception: ConfigValidationException) {
        throw CliInputException("Invalid $label store location: ${exception.message}")
    } catch (_: InvalidPathException) {
        throw CliInputException("Configured $label store could not be updated.")
    }

    private fun render(
        intent: OperatorActionIntent,
        workItemId: WorkItemId,
        approve: Boolean,
        outcome: ActOutcome,
    ): String = buildString {
        appendLine("Agent Desk")
        appendLine()
        when (outcome) {
            is ActOutcome.Executed -> {
                appendProposal(intent, workItemId, outcome.proposal.target.status, outcome.proposal.expectedEffect)
                appendDecision(outcome.decision.state.name, outcome.decision.logSummary)
                appendLine("Outcome")
                appendLine("- ${outcome.result.state.name}")
                appendLine("- ${outcome.result.replayStateSummary}")
                appendLine("- Recorded event: ${outcome.recordedEvent.id}")
                appendLine()
                appendAuditTrail(outcome.auditEntries)
            }

            is ActOutcome.NotExecuted -> {
                appendProposal(intent, workItemId, outcome.proposal.target.status, outcome.proposal.expectedEffect)
                appendDecision(outcome.decision.state.name, outcome.decision.logSummary)
                appendLine("Outcome")
                outcome.approvalState?.let { state -> appendLine("- ${state.name}") }
                appendLine("- No action was recorded. Audit evidence was still written.")
                appendLine()
                appendAuditTrail(outcome.auditEntries)
                appendLine()
                appendLine("Next step")
                appendLine("- ${nextStepFor(workItemId, approve, outcome)}")
            }

            ActOutcome.WorkItemNotFound -> appendLine("Work item was not found.")
        }
    }.trimEnd()

    private fun StringBuilder.appendProposal(
        intent: OperatorActionIntent,
        workItemId: WorkItemId,
        statusLabel: String,
        expectedEffect: String,
    ) {
        appendLine("Action proposal")
        appendLine("- ${intent.wireName} $workItemId ($statusLabel)")
        appendLine("- $expectedEffect")
        appendLine()
    }

    private fun StringBuilder.appendDecision(
        state: String,
        logSummary: String,
    ) {
        appendLine("Permission decision")
        appendLine("- $state")
        appendLine("- $logSummary")
        appendLine()
    }

    private fun StringBuilder.appendAuditTrail(entries: List<AuditEntry>) {
        appendLine("Audit trail (${entries.size} durable record(s))")
        AuditTrailProjector.timelineLines(entries).forEach { line ->
            appendLine("- ${line.timestamp} ${line.actor} ${line.action} ${line.target} ${line.result}")
        }
    }

    private fun nextStepFor(
        workItemId: WorkItemId,
        approve: Boolean,
        outcome: ActOutcome.NotExecuted,
    ): String = when {
        !approve && outcome.decision.behavior in approvalBehaviors ->
            "Re-run with --approve to confirm this local action explicitly."

        outcome.decision.behavior == PermissionGateBehavior.DenyUnavailable ->
            "This action is unavailable for the work item in the current proposal-only model."

        else -> "Inspect the work item for details: agent-desk inspect $workItemId"
    }

    private val cliActor = Actor.parse("operator:cli")

    private val approvalBehaviors = setOf(
        PermissionGateBehavior.RequireLocalConfirmation,
        PermissionGateBehavior.RequireExplicitApproval,
    )

    private const val POLICY_DENIED_EXIT_CODE = 3
}
