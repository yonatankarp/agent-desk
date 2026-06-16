package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.app.operator.EventLine
import com.yonatankarp.agentdesk.app.operator.EvidenceDisplayFormatter
import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.OperatorStatePresenter
import com.yonatankarp.agentdesk.app.operator.ProvenanceLine
import com.yonatankarp.agentdesk.app.operator.decision.DecisionQueueProjector

object DesktopEvidenceDrilldown {
    fun rows(screenState: DesktopScreenState): List<String> {
        val state = (screenState as? DesktopScreenState.Ready)?.state
        return when {
            state == null -> listOf(
                "Evidence unavailable until operator state is loaded.",
                "Diagnostics: raw provider data and arbitrary local file opening are unavailable by design.",
            )

            state.events.isEmpty() -> listOf(
                "Evidence missing: no replay events are available.",
                "Criteria result: ${screenState.criteriaResult()}",
                "Operator notes: unavailable in read-only proof.",
                "Diagnostics: raw provider data and arbitrary local file opening are unavailable by design.",
            )

            else -> rowsForLatestEvent(state, screenState)
        }
    }

    private fun rowsForLatestEvent(
        state: OperatorState,
        screenState: DesktopScreenState.Ready,
    ): List<String> {
        val event = state.events.last()
        val line = OperatorStatePresenter.eventLines(state).last()
        val decisions = DecisionQueueProjector.project(state).items
        val decision = decisions.lastOrNull { it.workItemId.toString() == line.workItemId }
        val relatedEvents = OperatorStatePresenter.eventLines(state).filter { it.workItemId == line.workItemId }
        val evidenceRows = line.evidenceRows()

        return buildList {
            add("Observation: ${line.type} for ${line.workItemId}")
            add("Source: ${line.source}")
            add("Timestamp: ${line.occurredAt}")
            add("Provenance: replay event ${event.id}")
            line.provenance?.summary()?.let { add("Provenance fields: $it") }
            addAll(evidenceRows)
            decision?.let {
                add("Decision: ${it.state.name} from ${it.request.source}")
                add("Decision unavailable: ${it.unavailableReason}")
            } ?: add("Decision: unavailable for latest replay event.")
            add("Criteria result: ${screenState.criteriaResult()}")
            add("Related events: ${relatedEvents.joinToString { related -> related.type }}")
            add("Derived summary: ${line.detail}")
            add("Agent interpretation: derived from sanitized replay state.")
            add("Operator notes: unavailable in read-only proof.")
            add("Redacted evidence: raw provider payloads are not rendered.")
            add("Diagnostics: raw provider data and arbitrary local file opening are unavailable by design.")
        }
    }

    private fun EventLine.evidenceRows(): List<String> = evidenceReferences
        .map { evidence ->
            "Primary evidence: ${EvidenceDisplayFormatter.format(evidence)}"
        }
        .ifEmpty {
            listOf("Evidence missing: no public-safe evidence reference was attached.")
        }

    private fun ProvenanceLine.summary(): String? = listOfNotNull(
        projectId,
        workspaceId,
        sourceId,
        ownerId,
        agentId,
        modelId,
        toolId,
        runId,
        objectiveId,
        parentHandoffId,
        archiveRecordId,
    ).takeIf { it.isNotEmpty() }?.joinToString(" ")

    private fun DesktopScreenState.criteriaResult(): String = DesktopReplayStatus.rows(this)
        .firstOrNull { row -> !row.startsWith("Read-only replay source:") }
        ?: "Criteria unavailable."
}
