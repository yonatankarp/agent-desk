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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yonatankarp.agentdesk.app.operator.OperatorDisplaySection
import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.OperatorStatePresenter
import com.yonatankarp.agentdesk.app.operator.SampleOperatorState
import com.yonatankarp.agentdesk.app.operator.StatusTone
import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.design.component.ActionRow
import com.yonatankarp.agentdesk.design.component.EventRow
import com.yonatankarp.agentdesk.design.component.EvidenceItem
import com.yonatankarp.agentdesk.design.component.Panel
import com.yonatankarp.agentdesk.design.component.ThemeModeControl
import com.yonatankarp.agentdesk.design.theme.AgentDeskTheme
import com.yonatankarp.agentdesk.design.theme.InMemoryThemeModeStore
import com.yonatankarp.agentdesk.design.theme.ThemeMode
import com.yonatankarp.agentdesk.design.theme.ThemeModeStore

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
fun AgentDeskApp(
    screenState: DesktopScreenState,
    themeStore: ThemeModeStore = InMemoryThemeModeStore(),
) {
    var mode by remember { mutableStateOf(themeStore.load()) }
    AgentDeskTheme(mode = mode) {
        Surface(modifier = Modifier.fillMaxSize(), color = AgentDeskTheme.colors.background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                DesktopHeader(screenState, mode) { next ->
                    mode = next
                    themeStore.save(next)
                }
                Panel(title = OperatorDisplaySection.ReplayStatus.desktopLabel, modifier = Modifier.fillMaxWidth()) {
                    DesktopReplayStatus.rows(screenState).forEach { row ->
                        Text(row, color = AgentDeskTheme.colors.textSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column(Modifier.weight(1.35f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Panel(title = OperatorDisplaySection.WorkState.desktopLabel, modifier = Modifier.weight(1f).fillMaxWidth()) {
                            val rows = screenState.workRows()
                            if (rows.isEmpty()) {
                                Text("No current work", color = AgentDeskTheme.colors.textMuted, fontSize = 13.sp)
                            } else {
                                rows.forEach { row -> DesktopWorkMetadataRow(row) }
                            }
                        }
                        Panel(title = OperatorDisplaySection.Timeline.desktopLabel, modifier = Modifier.weight(1f).fillMaxWidth()) {
                            val lines = DesktopTimelinePresenter.rows(screenState.readyState())
                            if (lines.isEmpty()) {
                                Text("No recent events", color = AgentDeskTheme.colors.textMuted, fontSize = 13.sp)
                            } else {
                                lines.forEachIndexed { i, line ->
                                    EventRow(
                                        type = line.type,
                                        occurredAt = line.occurredAt,
                                        detail = line.detail,
                                        source = line.source,
                                        showDivider = i < lines.lastIndex,
                                    )
                                }
                            }
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Panel(
                            title = OperatorDisplaySection.DecisionQueue.desktopLabel,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            titleColor = AgentDeskTheme.statusRole(StatusTone.Blocked).text,
                        ) {
                            val attention = screenState.attentionItems()
                            val message = screenState.message()
                            when {
                                message != null -> Text(message, color = AgentDeskTheme.colors.textMuted, fontSize = 13.sp)
                                attention.isEmpty() -> Text("No items need a decision", color = AgentDeskTheme.colors.textMuted, fontSize = 13.sp)
                                else -> screenState.attentionRows().forEach { row -> DesktopWorkMetadataRow(row) }
                            }
                        }
                        Panel(title = OperatorDisplaySection.EvidenceDetail.desktopLabel, modifier = Modifier.weight(1f).fillMaxWidth()) {
                            DesktopEvidenceDrilldown.rows(screenState).forEach { row -> EvidenceItem(label = row) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopHeader(
    screenState: DesktopScreenState,
    mode: ThemeMode,
    onCycle: (ThemeMode) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Agent Desk", color = AgentDeskTheme.colors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            Text("Local operator console", color = AgentDeskTheme.colors.textMuted, fontSize = 14.sp)
            Text(screenState.modeLabel, color = AgentDeskTheme.colors.textMuted, fontFamily = AgentDeskTheme.typography.monoFamily, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(screenState.summaryText(), color = AgentDeskTheme.colors.textMuted, fontFamily = AgentDeskTheme.typography.monoFamily, fontSize = 13.sp)
            ThemeModeControl(mode = mode, onCycle = onCycle)
        }
    }
}

@Composable
private fun DesktopWorkMetadataRow(row: DesktopWorkItemRow) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ActionRow(id = row.id, title = row.title, tone = row.tone, statusLabel = row.statusLabel)
        row.summary?.let { summary ->
            Text(summary, color = AgentDeskTheme.colors.textSecondary, fontSize = 12.sp, lineHeight = 17.sp)
        }
        row.evidenceText?.let { evidence ->
            Text(
                "Evidence: $evidence",
                color = AgentDeskTheme.colors.textMuted,
                fontFamily = AgentDeskTheme.typography.monoFamily,
                fontSize = 10.sp,
                lineHeight = 15.sp,
            )
        }
        row.staleText?.let { stale ->
            Text(
                stale,
                color = AgentDeskTheme.statusRole(StatusTone.Attention).text,
                fontFamily = AgentDeskTheme.typography.monoFamily,
                fontSize = 10.sp,
                lineHeight = 15.sp,
            )
        }
    }
}

private fun DesktopScreenState.readyState(): OperatorState? = (this as? DesktopScreenState.Ready)?.state

private fun DesktopScreenState.attentionItems(): List<WorkItem> = DesktopWorkItemPresenter.attentionItems(readyState())

private fun DesktopScreenState.workRows(): List<DesktopWorkItemRow> {
    val state = readyState()
    return DesktopWorkItemPresenter.rows(state, state?.workItems.orEmpty(), includeStale = false)
}

private fun DesktopScreenState.attentionRows(): List<DesktopWorkItemRow> = DesktopWorkItemPresenter.rows(readyState(), attentionItems(), includeStale = true)

private fun DesktopScreenState.message(): String? = when (this) {
    DesktopScreenState.Loading -> "Loading operator state"
    is DesktopScreenState.Error -> message
    is DesktopScreenState.Ready -> null
}

private fun DesktopScreenState.summaryText(): String {
    val state = readyState() ?: return "0 active / 0 attention"
    return "${OperatorStatePresenter.activeCount(state)} active / ${DesktopWorkItemPresenter.attentionItems(state).size} attention"
}
