package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus

object ReplayStatusPresenter {
    fun readyRows(
        state: OperatorState,
        modeLabel: String,
    ): List<String> {
        val attentionItems = OperatorStatePresenter.attentionItems(state)
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
            add("Import diagnostics: available from the canonical replay smoke command.")
            add("Discovery/no-issue output is triage, not product completion.")
        }
    }

    fun criteriaResult(
        state: OperatorState,
        modeLabel: String,
    ): String = readyRows(state, modeLabel)
        .firstOrNull { row -> !row.startsWith("Read-only replay source:") }
        ?: "Criteria unavailable."
}
