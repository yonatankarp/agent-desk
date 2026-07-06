package com.yonatankarp.agentdesk.app.operator.audit

import com.yonatankarp.agentdesk.app.operator.action.ActionPermissionDecision
import com.yonatankarp.agentdesk.app.operator.action.MockActionApprovalResult
import com.yonatankarp.agentdesk.app.persistence.AuditRecordRepository
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp

/**
 * Appends projected audit entries to the durable audit store. The permission
 * gate and approval loop stay pure; this recorder is the seam callers route
 * outcomes through so every decision — including denials and failures — is
 * persisted. The CLI act path is the first production caller.
 */
class AuditTrailRecorder(
    private val repository: AuditRecordRepository,
) {
    fun record(
        decision: ActionPermissionDecision,
        recordedAt: EventTimestamp,
    ): AuditEntry = AuditTrailProjector.fromPermissionDecision(decision, recordedAt = recordedAt)
        .also(repository::append)

    fun record(
        result: MockActionApprovalResult,
        recordedAt: EventTimestamp,
    ): List<AuditEntry> = AuditTrailProjector.fromMockActionResult(result, recordedAt = recordedAt)
        .onEach(repository::append)

    fun record(entry: AuditEntry): AuditEntry = entry.also(repository::append)
}
