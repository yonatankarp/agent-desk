package com.yonatankarp.agentdesk.app.fixtures

import com.yonatankarp.agentdesk.app.operator.Actor
import com.yonatankarp.agentdesk.app.operator.audit.AuditActorKind
import com.yonatankarp.agentdesk.app.operator.audit.AuditEntry
import com.yonatankarp.agentdesk.app.operator.audit.AuditResult
import com.yonatankarp.agentdesk.core.domain.events.EvidenceLabel
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReference
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReferenceKind
import com.yonatankarp.agentdesk.core.domain.events.EvidenceTarget
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.testfixtures.eventTimestampAt

/**
 * Deterministic audit-entry factory on the canonical fixture day. Vary only the
 * fields a test pins; ids stay unique per (result, minute) so append tests can
 * write several entries without colliding.
 */
internal fun auditEntry(
    result: AuditResult = AuditResult.Approved,
    actorKind: AuditActorKind = AuditActorKind.Human,
    minute: Int = 22,
    id: String = "audit:agent-task:42:resume:${result.name.lowercase()}:${eventTimestampAt(minute = minute)}",
): AuditEntry = AuditEntry(
    id = id,
    actor = Actor.parse("operator:daily-agent"),
    actorKind = actorKind,
    timestamp = eventTimestampAt(minute = minute),
    recordedAt = eventTimestampAt(minute = minute + 1),
    action = "decision.approve-resume",
    target = WorkItemId.parse("agent-task:42"),
    result = result,
    sourceItem = WorkItemId.parse("agent-task:42"),
    correlationId = "correlation:agent-task:42:resume:${eventTimestampAt(minute = minute)}",
    evidenceReference = EvidenceReference(
        kind = EvidenceReferenceKind.SanitizedNote,
        label = EvidenceLabel.parse("Mock action ${result.name}"),
        target = EvidenceTarget.parse("mock-action:resume:${result.name.lowercase()}"),
    ),
    detail = "Public-safe mock approval.",
)
