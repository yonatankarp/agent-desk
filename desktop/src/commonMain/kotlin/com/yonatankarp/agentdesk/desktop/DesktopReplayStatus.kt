package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.app.operator.OperatorStatePresenter
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus

object DesktopReplayStatus {
    fun rows(screenState: DesktopScreenState): List<String> = when (screenState) {
        DesktopScreenState.Loading -> listOf("Replay status: loading operator state.")

        is DesktopScreenState.Error -> listOf("Blocked/error: ${screenState.message}")

        is DesktopScreenState.Ready -> {
            val attentionItems = OperatorStatePresenter.attentionItems(screenState.state)
            buildList {
                add("Read-only replay source: ${screenState.modeLabel}.")
                if (screenState.state.workItems.isEmpty() && screenState.state.events.isEmpty()) {
                    add("Empty queue: no current work or decisions; not product completion without milestone evidence.")
                } else if (attentionItems.isNotEmpty()) {
                    add("Not done: ${attentionItems.size} item(s) need operator attention.")
                } else {
                    add("Decision queue: no items need operator attention.")
                }
                if (screenState.state.workItems.any { it.status == WorkStatus.Succeeded }) {
                    add("Successful outcome visible in the read-only timeline.")
                }
                add("Import diagnostics: available from the canonical replay smoke command.")
                add("Discovery/no-issue output is triage, not product completion.")
            }
        }
    }
}
