package com.yonatankarp.agentdesk.app.operator.notification

import com.yonatankarp.agentdesk.app.operator.EvidenceLine
import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventType
import com.yonatankarp.agentdesk.core.domain.projections.StaleWorkAttention
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus

data class NotificationRuleProjection(
    val deliveryMode: NotificationDeliveryMode,
    val signals: List<NotificationSignal>,
    val digestGroups: List<NotificationDigestGroup>,
) {
    val immediateCandidates: List<NotificationSignal> = signals.filter { signal ->
        deliveryMode == NotificationDeliveryMode.ReadOnly || signal.urgency in immediateUrgencies
    }

    private companion object {
        private val immediateUrgencies = setOf(NotificationUrgency.High, NotificationUrgency.Critical)
    }
}

data class NotificationSignal(
    val rule: NotificationRule,
    val workItemId: WorkItemId,
    val title: String,
    val recordedAt: EventTimestamp,
    val urgency: NotificationUrgency,
    val digestGroup: DigestGroup,
    val dedupeKey: String,
    val reason: String,
    val evidenceReferences: List<EvidenceLine> = emptyList(),
    val occurrenceCount: Int = 1,
)

data class NotificationDigestGroup(
    val group: DigestGroup,
    val signals: List<NotificationSignal>,
)

data class DigestWindow(
    val startsAt: EventTimestamp,
    val endsAt: EventTimestamp,
) {
    init {
        require(startsAt <= endsAt) { "Digest window start must not be after its end" }
    }

    fun contains(timestamp: EventTimestamp): Boolean = timestamp >= startsAt && timestamp <= endsAt
}

enum class NotificationDeliveryMode {
    ReadOnly,
    Quiet,
}

enum class NotificationRule(val key: String) {
    DecisionRequested("decision-requested"),
    WorkBlocked("work-blocked"),
    WorkStale("work-stale"),
    WorkFailed("work-failed"),
    WorkCompleted("work-completed"),
    MaterialChange("material-change"),
}

enum class NotificationUrgency {
    Low,
    Normal,
    High,
    Critical,
}

enum class DigestGroup {
    CompletedWork,
    NewBlockers,
    PendingDecisions,
    MaterialChanges,
}

object NotificationRuleProjector {
    fun project(
        state: OperatorState,
        deliveryMode: NotificationDeliveryMode = NotificationDeliveryMode.ReadOnly,
        window: DigestWindow? = null,
    ): NotificationRuleProjection {
        val eventsByWorkItem = state.events.groupBy { it.workItemId }
        val latestEventByWorkItem = eventsByWorkItem.mapValues { (_, events) -> events.maxBy { it.occurredAt } }
        val workItemsById = state.workItems.associateBy { it.id }
        val signals = (
            state.workItems.mapNotNull { item -> item.toLifecycleSignal(eventsByWorkItem[item.id].orEmpty()) } +
                state.staleAttention.mapNotNull { stale -> stale.toSignal(workItemsById[stale.workItemId]) } +
                latestEventByWorkItem.values.mapNotNull { event -> event.toMaterialChangeSignal(workItemsById[event.workItemId]) }
            )
            .dedupeLatest()
            .filter { signal -> window?.contains(signal.recordedAt) ?: true }
            .sortedWith(compareBy<NotificationSignal> { it.recordedAt }.thenBy { it.dedupeKey })

        return NotificationRuleProjection(
            deliveryMode = deliveryMode,
            signals = signals,
            digestGroups = signals
                .groupBy { it.digestGroup }
                .map { (group, groupSignals) -> NotificationDigestGroup(group = group, signals = groupSignals) }
                .sortedBy { it.group.ordinal },
        )
    }

    private fun WorkItem.toLifecycleSignal(events: List<WorkEvent>): NotificationSignal? = when (status) {
        WorkStatus.NeedsDecision -> signal(
            rule = NotificationRule.DecisionRequested,
            events = events,
            urgency = NotificationUrgency.High,
            digestGroup = DigestGroup.PendingDecisions,
            reason = "Operator decision is required before work can continue.",
        )

        WorkStatus.Blocked -> signal(
            rule = NotificationRule.WorkBlocked,
            events = events,
            urgency = NotificationUrgency.High,
            digestGroup = DigestGroup.NewBlockers,
            reason = "Work is blocked and needs operator attention.",
        )

        WorkStatus.Failed -> signal(
            rule = NotificationRule.WorkFailed,
            events = events,
            urgency = NotificationUrgency.Critical,
            digestGroup = DigestGroup.NewBlockers,
            reason = "Work reached a failed terminal state.",
        )

        WorkStatus.Succeeded -> signal(
            rule = NotificationRule.WorkCompleted,
            events = events,
            urgency = NotificationUrgency.Low,
            digestGroup = DigestGroup.CompletedWork,
            reason = "Work completed successfully.",
        )

        WorkStatus.Queued,
        WorkStatus.Running,
        WorkStatus.Waiting,
        WorkStatus.Canceled,
        -> null
    }

    private fun StaleWorkAttention.toSignal(item: WorkItem?): NotificationSignal? {
        if (item == null) return null
        return NotificationSignal(
            rule = NotificationRule.WorkStale,
            workItemId = workItemId,
            title = item.title.toString(),
            recordedAt = lastEventAt,
            urgency = NotificationUrgency.Normal,
            digestGroup = DigestGroup.MaterialChanges,
            dedupeKey = dedupeKey(NotificationRule.WorkStale, workItemId),
            reason = "${status.name} work has had no accepted event for $staleForMinutes minutes.",
        )
    }

    private fun WorkEvent.toMaterialChangeSignal(item: WorkItem?): NotificationSignal? {
        if (item == null || evidenceReferences.isEmpty() || item.status.requiresHumanAttention || item.status.isTerminal) {
            return null
        }
        return NotificationSignal(
            rule = NotificationRule.MaterialChange,
            workItemId = workItemId,
            title = item.title.toString(),
            recordedAt = occurredAt,
            urgency = NotificationUrgency.Normal,
            digestGroup = DigestGroup.MaterialChanges,
            dedupeKey = dedupeKey(NotificationRule.MaterialChange, workItemId),
            reason = "New public-safe evidence is available for active work.",
            evidenceReferences = evidenceLines(),
        )
    }

    private fun WorkItem.signal(
        rule: NotificationRule,
        events: List<WorkEvent>,
        urgency: NotificationUrgency,
        digestGroup: DigestGroup,
        reason: String,
    ): NotificationSignal? {
        val matchingEvents = events.filter { rule.matches(it) }
        val event = matchingEvents.maxByOrNull { it.occurredAt } ?: return null
        return NotificationSignal(
            rule = rule,
            workItemId = id,
            title = title.toString(),
            recordedAt = event.occurredAt,
            urgency = urgency,
            digestGroup = digestGroup,
            dedupeKey = dedupeKey(rule, id),
            reason = reason,
            evidenceReferences = event.evidenceLines(),
            occurrenceCount = matchingEvents.count(),
        )
    }

    private fun List<NotificationSignal>.dedupeLatest(): List<NotificationSignal> = groupBy { it.dedupeKey }
        .values
        .map { signals ->
            signals.maxBy { it.recordedAt }.copy(occurrenceCount = signals.sumOf { it.occurrenceCount })
        }

    private fun NotificationRule.matches(event: WorkEvent): Boolean = when (this) {
        NotificationRule.DecisionRequested -> event.type == WorkEventType.WorkNeedsDecision

        NotificationRule.WorkBlocked -> event.type == WorkEventType.WorkBlocked

        NotificationRule.WorkFailed -> event.type == WorkEventType.WorkFailed

        NotificationRule.WorkCompleted -> event.type == WorkEventType.WorkSucceeded

        NotificationRule.WorkStale,
        NotificationRule.MaterialChange,
        -> false
    }

    private fun WorkEvent.evidenceLines(): List<EvidenceLine> = evidenceReferences.map { evidence ->
        EvidenceLine(
            kind = evidence.kind.wireName,
            label = evidence.label.toString(),
            target = evidence.target.toString(),
        )
    }

    private fun dedupeKey(
        rule: NotificationRule,
        workItemId: WorkItemId,
    ): String = "${rule.key}:$workItemId"
}
