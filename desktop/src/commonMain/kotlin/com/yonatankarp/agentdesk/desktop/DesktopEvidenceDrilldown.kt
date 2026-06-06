package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.OperatorStatePresenter

object DesktopEvidenceDrilldown {
    fun rows(screenState: DesktopScreenState): List<String> {
        val state = (screenState as? DesktopScreenState.Ready)?.state
        return when {
            state == null -> listOf("Evidence unavailable until operator state is loaded.")
            state.events.isEmpty() -> listOf("Evidence missing: no replay events are available.")
            else -> rowsForLatestEvent(state)
        }
    }

    private fun rowsForLatestEvent(state: OperatorState): List<String> {
        val event = state.events.last()
        val line = OperatorStatePresenter.eventLines(state).last()
        val evidenceRows = line.evidenceReferences.map { evidence ->
            "Primary evidence: ${evidence.kind} ${evidence.label} -> ${evidence.target}"
        }.ifEmpty {
            listOf("Evidence missing: no public-safe evidence reference was attached.")
        }

        return buildList {
            add("Observation: ${line.type} for ${line.workItemId}")
            add("Source: ${line.source}")
            add("Timestamp: ${line.occurredAt}")
            add("Provenance: replay event ${event.id}")
            add("Derived summary: ${line.detail}")
            addAll(evidenceRows)
            add("Agent interpretation: derived from sanitized replay state.")
            add("Operator notes: unavailable in read-only proof.")
            add("Diagnostics: raw provider data and arbitrary local file opening are unavailable by design.")
        }
    }
}
