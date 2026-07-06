package com.yonatankarp.agentdesk.app.operator.action

import com.yonatankarp.agentdesk.app.operator.Actor
import com.yonatankarp.agentdesk.app.operator.OperatorStateProjector
import com.yonatankarp.agentdesk.app.operator.audit.AuditEntry
import com.yonatankarp.agentdesk.app.operator.audit.AuditResult
import com.yonatankarp.agentdesk.app.operator.audit.AuditTrailProjector
import com.yonatankarp.agentdesk.app.operator.audit.AuditTrailRecorder
import com.yonatankarp.agentdesk.app.persistence.WorkEventRepository
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostAccessBoundary
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostOperation
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId

class LiveHostInspectAction(
    private val eventRepository: WorkEventRepository,
    private val auditRecorder: AuditTrailRecorder,
    private val hostBoundary: RuntimeHostAccessBoundary,
    private val adapter: LiveHostInspectAdapter,
    private val expiresAt: EventTimestamp,
) {
    fun inspect(
        workItemId: WorkItemId,
        actor: Actor,
        approval: LiveHostInspectApproval,
        now: EventTimestamp,
    ): LiveHostInspectOutcome {
        val item = OperatorStateProjector.project(eventRepository.readAll().events)
            .workItems
            .firstOrNull { it.id == workItemId }
            ?: return LiveHostInspectOutcome.WorkItemNotFound

        val hostAlias = hostBoundary.alias
            ?: return LiveHostInspectOutcome.NotExecuted(
                reason = "Inspect host alias is unavailable.",
                auditEntries = emptyList(),
            )
        val proposal = LiveHostInspectProposal(
            proposalId = LiveHostInspectProposal.idFor(workItemId, now),
            correlationId = "correlation:$workItemId:inspect:$now",
            hostAlias = hostAlias,
            target = workItemId,
            summary = "Inspect ${item.title} on ${hostAlias.value}.",
            createdAt = now,
            expiresAt = expiresAt,
        )
        val auditEntries = mutableListOf<AuditEntry>()
        auditEntries += auditRecorder.record(proposal.auditEntry(actor, now, "proposal.created", AuditResult.Deferred))

        if (!hostBoundary.allows(RuntimeHostOperation.InspectActionProposal)) {
            auditEntries += auditRecorder.record(proposal.auditEntry(actor, now, "approval.unavailable", AuditResult.Rejected))
            return LiveHostInspectOutcome.NotExecuted(
                reason = "Host boundary does not allow inspect action proposals.",
                auditEntries = auditEntries,
            )
        }

        if (now >= expiresAt) {
            auditEntries += auditRecorder.record(proposal.auditEntry(actor, now, "approval.expired", AuditResult.Rejected))
            return LiveHostInspectOutcome.NotExecuted(
                reason = "Inspect proposal expired before approval.",
                auditEntries = auditEntries,
            )
        }

        when (approval) {
            is LiveHostInspectApproval.Deny -> {
                auditEntries += auditRecorder.record(proposal.auditEntry(actor, now, "approval.denied", AuditResult.Rejected))
                return LiveHostInspectOutcome.NotExecuted(
                    reason = "Inspect proposal denied by operator.",
                    auditEntries = auditEntries,
                )
            }

            is LiveHostInspectApproval.Approve -> if (!approval.matches(proposal)) {
                auditEntries += auditRecorder.record(proposal.auditEntry(actor, now, "approval.mismatched", AuditResult.Rejected))
                return LiveHostInspectOutcome.NotExecuted(
                    reason = "Inspect approval did not match the exact proposal.",
                    auditEntries = auditEntries,
                )
            }
        }

        auditEntries += auditRecorder.record(proposal.auditEntry(actor, now, "approval.approved", AuditResult.Approved))
        auditEntries += auditRecorder.record(proposal.auditEntry(actor, now, "adapter.started", AuditResult.Deferred))

        return when (val result = adapter.inspect(proposal.toAdapterRequest())) {
            is LiveHostInspectAdapterResult.Succeeded -> {
                auditEntries += auditRecorder.record(proposal.auditEntry(actor, now, "adapter.succeeded", AuditResult.Approved))
                auditEntries += auditRecorder.record(
                    proposal.auditEntry(
                        actor = actor,
                        recordedAt = now,
                        phase = "output.rendered",
                        result = AuditResult.Approved,
                        detail = "Rendered sanitized inspect output: ${result.output.summary}",
                    ),
                )
                LiveHostInspectOutcome.Executed(
                    proposal = proposal,
                    output = result.output,
                    auditEntries = auditEntries,
                )
            }

            is LiveHostInspectAdapterResult.Failed -> {
                auditEntries += auditRecorder.record(
                    proposal.auditEntry(actor, now, "adapter.failed", AuditResult.Failed, detail = result.reason),
                )
                LiveHostInspectOutcome.NotExecuted(
                    reason = result.reason,
                    auditEntries = auditEntries,
                )
            }

            is LiveHostInspectAdapterResult.UnsafeOutput -> {
                auditEntries += auditRecorder.record(
                    proposal.auditEntry(actor, now, "adapter.unsafe-rejected", AuditResult.Failed, detail = result.reason),
                )
                LiveHostInspectOutcome.NotExecuted(
                    reason = result.reason,
                    auditEntries = auditEntries,
                )
            }
        }
    }

    private fun LiveHostInspectApproval.Approve.matches(proposal: LiveHostInspectProposal): Boolean = proposalId == proposal.proposalId &&
        hostAlias == proposal.hostAlias &&
        target == proposal.target &&
        action == proposal.action

    private fun LiveHostInspectProposal.toAdapterRequest(): LiveHostInspectAdapterRequest = LiveHostInspectAdapterRequest(
        proposalId = proposalId,
        correlationId = correlationId,
        hostAlias = hostAlias,
        target = target,
    )

    private fun LiveHostInspectProposal.auditEntry(
        actor: Actor,
        recordedAt: EventTimestamp,
        phase: String,
        result: AuditResult,
        detail: String = defaultDetailFor(phase),
    ): AuditEntry = AuditTrailProjector.liveHostInspectEntry(
        proposal = this,
        actor = actor,
        recordedAt = recordedAt,
        phase = phase,
        result = result,
        detail = detail,
    )

    private fun LiveHostInspectProposal.defaultDetailFor(phase: String): String = when (phase) {
        "proposal.created" -> "Created inspect proposal for $target on ${hostAlias.value}."
        "approval.approved" -> "Operator approved inspect proposal $proposalId."
        "approval.denied" -> "Operator denied inspect proposal $proposalId."
        "approval.mismatched" -> "Inspect approval did not match the exact proposal."
        "approval.unavailable" -> "Inspect proposal is unavailable for the current host boundary."
        "approval.expired" -> "Inspect proposal expired before execution."
        "adapter.started" -> "Started inspect adapter after explicit approval."
        "adapter.succeeded" -> "Inspect adapter returned sanitized output."
        "adapter.failed" -> "Inspect adapter failed with a public-safe error."
        "adapter.unsafe-rejected" -> "Inspect adapter output failed public-safety validation."
        else -> "Recorded inspect action audit phase $phase."
    }
}
