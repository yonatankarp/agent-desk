@file:Suppress("FunctionName")

package com.yonatankarp.agentdesk.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yonatankarp.agentdesk.app.operator.EventLine
import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.OperatorStatePresenter
import com.yonatankarp.agentdesk.app.operator.SampleOperatorState
import com.yonatankarp.agentdesk.app.operator.StatusTone
import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.design.component.ActionRow
import com.yonatankarp.agentdesk.design.component.EvidenceItem
import com.yonatankarp.agentdesk.design.component.EventRow
import com.yonatankarp.agentdesk.design.component.Panel
import com.yonatankarp.agentdesk.design.theme.AgentDeskTheme
import com.yonatankarp.agentdesk.design.theme.ThemeMode

@Composable
@Preview
fun previewAgentDeskApp() {
    AgentDeskApp(DesktopScreenState.Ready(SampleOperatorState.current(), modeLabel = "Sample state"))
}

@Composable
fun AgentDeskApp(state: OperatorState) {
    AgentDeskApp(DesktopScreenState.Ready(state, modeLabel = "Sample state"))
}

@Composable
fun AgentDeskApp(screenState: DesktopScreenState) {
    AgentDeskTheme(mode = ThemeMode.System) {
        Surface(modifier = Modifier.fillMaxSize(), color = AgentDeskTheme.colors.background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                DesktopHeader(screenState)
                Panel(title = "Replay status", modifier = Modifier.fillMaxWidth()) {
                    DesktopReplayStatus.rows(screenState).forEach { row ->
                        Text(row, color = AgentDeskTheme.colors.textSecondary, fontSize = 13.sp)
                    }
                }
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column(Modifier.weight(1.35f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Panel(title = "Work state", modifier = Modifier.weight(1f).fillMaxWidth()) {
                            val items = screenState.readyState()?.workItems.orEmpty()
                            if (items.isEmpty()) {
                                Text("No current work", color = AgentDeskTheme.colors.textMuted, fontSize = 13.sp)
                            } else {
                                items.forEach { item ->
                                    val p = OperatorStatePresenter.presentationFor(item.status)
                                    ActionRow(id = item.id.toString(), title = item.title.toString(), tone = p.tone, statusLabel = p.label)
                                }
                            }
                        }
                        Panel(title = "Read-only timeline", modifier = Modifier.weight(1f).fillMaxWidth()) {
                            val lines = screenState.eventLines()
                            if (lines.isEmpty()) {
                                Text("No recent events", color = AgentDeskTheme.colors.textMuted, fontSize = 13.sp)
                            } else {
                                lines.forEachIndexed { i, line ->
                                    EventRow(
                                        type = line.type,
                                        occurredAt = line.occurredAt,
                                        detail = line.detail,
                                        source = "${line.workItemId} from ${line.source}",
                                        showDivider = i < lines.lastIndex,
                                    )
                                }
                            }
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Panel(
                            title = "Decision queue",
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            titleColor = AgentDeskTheme.statusRole(StatusTone.Blocked).text,
                        ) {
                            val attention = screenState.attentionItems()
                            val message = screenState.message()
                            when {
                                message != null -> Text(message, color = AgentDeskTheme.colors.textMuted, fontSize = 13.sp)
                                attention.isEmpty() -> Text("No items need a decision", color = AgentDeskTheme.colors.textMuted, fontSize = 13.sp)
                                else -> attention.forEach { item ->
                                    val p = OperatorStatePresenter.presentationFor(item.status)
                                    ActionRow(id = item.id.toString(), title = item.title.toString(), tone = p.tone, statusLabel = p.label)
                                }
                            }
                        }
                        Panel(title = "Evidence drilldown", modifier = Modifier.weight(1f).fillMaxWidth()) {
                            DesktopEvidenceDrilldown.rows(screenState).forEach { row -> EvidenceItem(label = row) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopHeader(screenState: DesktopScreenState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Agent Desk", color = AgentDeskTheme.colors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            Text("Local operator console", color = AgentDeskTheme.colors.textMuted, fontSize = 14.sp)
            Text(screenState.modeLabel, color = AgentDeskTheme.colors.textMuted, fontFamily = AgentDeskTheme.typography.monoFamily, fontSize = 12.sp)
        }
        Text(screenState.summaryText(), color = AgentDeskTheme.colors.textMuted, fontFamily = AgentDeskTheme.typography.monoFamily, fontSize = 13.sp)
    }
}

private fun DesktopScreenState.readyState(): OperatorState? = (this as? DesktopScreenState.Ready)?.state

private fun DesktopScreenState.attentionItems(): List<WorkItem> = readyState()?.let(OperatorStatePresenter::attentionItems).orEmpty()

private fun DesktopScreenState.eventLines(): List<EventLine> = readyState()?.let(OperatorStatePresenter::eventLines).orEmpty()

private fun DesktopScreenState.message(): String? = when (this) {
    DesktopScreenState.Loading -> "Loading operator state"
    is DesktopScreenState.Error -> message
    is DesktopScreenState.Ready -> null
}

private fun DesktopScreenState.summaryText(): String {
    val state = readyState() ?: return "0 active / 0 attention"
    return "${OperatorStatePresenter.activeCount(state)} active / ${OperatorStatePresenter.attentionItems(state).size} attention"
}
