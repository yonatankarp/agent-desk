package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus

object ReplayStatusPresenter {
    fun readyRows(
        state: OperatorState,
        modeLabel: String,
    ): List<String> {
        val attentionItems = OperatorStatePresenter.attentionItems(state)
        val health = OperatorHealthProjector.project(state, sourceLabel = modeLabel)
        return buildList {
            add("Read-only replay source: $modeLabel.")
            if (state.workItems.isEmpty() && state.events.isEmpty()) {
                add("Empty queue: no current work or decisions; not product completion without milestone evidence.")
            } else if (attentionItems.isNotEmpty()) {
                add("Not done: ${attentionItems.size} item(s) need operator attention.")
            } else {
                add("Decision queue: no items need operator attention.")
            }
            if (state.workItems.any { it.status == WorkStatus.Succeeded }) {
                add("Successful outcome visible in the read-only timeline.")
            }
            add("Health: ${health.status.label}.")
            add(health.ingestion)
            add(health.source)
            add(health.backend)
            add(health.lastEvent)
            add(health.lastReplay)
            add(health.nextSafeAction)
            health.diagnostics.forEach { diagnostic ->
                add("Diagnostic: $diagnostic")
            }
            add("Import diagnostics: available from the canonical replay smoke command.")
            add("Discovery/no-issue output is triage, not product completion.")
        }
    }

    fun failureRows(publicSafeMessage: String): List<String> {
        val health = OperatorHealthProjector.failedImportSurface(publicSafeMessage)
        return listOf(
            "Health: ${health.status.label}.",
            health.ingestion,
            health.source,
            health.backend,
            health.lastEvent,
            health.lastReplay,
            health.nextSafeAction,
        ) + health.diagnostics.map { diagnostic -> "Diagnostic: $diagnostic" }
    }

    fun criteriaResult(
        state: OperatorState,
        modeLabel: String,
    ): String = readyRows(state, modeLabel)
        .firstOrNull { row -> !row.startsWith("Read-only replay source:") }
        ?: "Criteria unavailable."
}
