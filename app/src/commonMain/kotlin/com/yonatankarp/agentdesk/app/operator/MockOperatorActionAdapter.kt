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
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary

class MockOperatorActionAdapter {
    fun perform(
        intent: OperatorActionIntent,
        workItemId: WorkItemId,
        events: List<WorkEvent>,
        occurredAt: EventTimestamp,
    ): WorkEvent {
        if (intent != OperatorActionIntent.Resume) {
            throw OperatorActionException("Mock action adapter currently supports only resume.")
        }

        val state = OperatorStateProjector.project(events)
        val item = state.workItems.firstOrNull { it.id == workItemId }
            ?: throw OperatorActionException("Work item was not found.")

        if (!item.status.isResumable) {
            throw OperatorActionException("Work item cannot be resumed from its current status.")
        }

        return WorkEvent(
            // The occurredAt segment keeps action event ids unique per invocation.
            id = WorkEventId.parse("event:$workItemId:action-resume:$occurredAt"),
            occurredAt = occurredAt,
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
    }
}
