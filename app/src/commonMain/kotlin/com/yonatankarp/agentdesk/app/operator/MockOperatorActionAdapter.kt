package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.core.domain.events.EventSource
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.EvidenceLabel
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReference
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReferenceKind
import com.yonatankarp.agentdesk.core.domain.events.EvidenceTarget
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary

class MockOperatorActionAdapter {
    fun perform(
        intent: OperatorActionIntent,
        workItemId: WorkItemId,
        events: List<WorkEvent>,
    ): WorkEvent {
        if (intent != OperatorActionIntent.Resume) {
            throw OperatorActionException("Mock action adapter currently supports only resume.")
        }

        val state = OperatorStateProjector.project(events)
        val item = state.workItems.firstOrNull { it.id == workItemId }
            ?: throw OperatorActionException("Work item was not found.")

        if (item.status !in resumableStatuses) {
            throw OperatorActionException("Work item cannot be resumed from its current status.")
        }

        return WorkEvent(
            id = WorkEventId.parse("event:$workItemId:action-resume"),
            occurredAt = EventTimestamp.parse("2026-06-02T21:20:00Z"),
            source = source,
            workItemId = workItemId,
            payload = WorkStartedPayload(
                title = item.title,
                summary = WorkSummary.parse("Mock operator requested resume."),
            ),
            evidenceReferences = listOf(
                EvidenceReference(
                    kind = EvidenceReferenceKind.SanitizedNote,
                    label = EvidenceLabel.parse("Mock resume action"),
                    target = EvidenceTarget.parse("mock-action:resume"),
                ),
            ),
        )
    }

    companion object {
        private val source = EventSource.parse("mock-action-adapter")
        private val resumableStatuses = setOf(WorkStatus.NeedsDecision, WorkStatus.Blocked, WorkStatus.Waiting)
    }
}
