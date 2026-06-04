package com.yonatankarp.agentdesk.app.runtime

import com.yonatankarp.agentdesk.core.domain.events.WorkEvent

class MockRuntimeWorkEventSource(
    private val mapper: SanitizedRuntimeObservationMapper = SanitizedRuntimeObservationMapper(),
) : RuntimeWorkEventSource {
    override fun loadEvents(): List<WorkEvent> = observations.map(mapper::toWorkEvent)

    companion object {
        val observations =
            listOf(
                RuntimeWorkObservation(
                    eventId = "event:agent-task:42:started",
                    occurredAt = "2026-06-02T21:00:00Z",
                    source = "mock-adapter",
                    workItemId = "agent-task:42",
                    kind = RuntimeWorkObservationKind.Started,
                    title = "Run public hygiene check",
                    summary = "Agent accepted the task and started local checks.",
                ),
                RuntimeWorkObservation(
                    eventId = "event:agent-task:44:blocked",
                    occurredAt = "2026-06-02T21:05:00Z",
                    source = "mock-adapter",
                    workItemId = "agent-task:44",
                    kind = RuntimeWorkObservationKind.Blocked,
                    reason = "CI failed on the core test task.",
                ),
                RuntimeWorkObservation(
                    eventId = "event:agent-task:45:needs-decision",
                    occurredAt = "2026-06-02T21:10:00Z",
                    source = "mock-adapter",
                    workItemId = "agent-task:45",
                    kind = RuntimeWorkObservationKind.NeedsDecision,
                    reason = "Operator must choose whether to retry the failed check.",
                ),
                RuntimeWorkObservation(
                    eventId = "event:agent-task:42:succeeded",
                    occurredAt = "2026-06-02T21:15:00Z",
                    source = "mock-adapter",
                    workItemId = "agent-task:42",
                    kind = RuntimeWorkObservationKind.Succeeded,
                ),
            )
    }
}
