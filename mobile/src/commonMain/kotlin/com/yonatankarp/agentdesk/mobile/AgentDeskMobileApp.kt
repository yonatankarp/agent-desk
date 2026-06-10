@file:Suppress("FunctionName")

package com.yonatankarp.agentdesk.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yonatankarp.agentdesk.app.operator.StatusTone
import com.yonatankarp.agentdesk.app.operator.mobile.MobileAttentionItem
import com.yonatankarp.agentdesk.app.operator.mobile.MobileEventLine
import com.yonatankarp.agentdesk.app.operator.mobile.MobileEvidenceDetail
import com.yonatankarp.agentdesk.app.operator.mobile.MobileOperatorState
import com.yonatankarp.agentdesk.app.operator.mobile.MobileOperatorStateContract
import com.yonatankarp.agentdesk.app.operator.mobile.MobileProjectionWarning
import com.yonatankarp.agentdesk.app.operator.mobile.MobileTimelineEntry
import com.yonatankarp.agentdesk.app.operator.mobile.MobileWorkItem
import com.yonatankarp.agentdesk.design.component.Panel
import com.yonatankarp.agentdesk.design.theme.AgentDeskTheme
import com.yonatankarp.agentdesk.design.theme.ThemeMode

@Composable
fun AgentDeskMobileApp(state: MobileOperatorState = MobileOperatorStateContract.sample()) {
    AgentDeskTheme(mode = ThemeMode.System) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AgentDeskTheme.colors.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                MobileHeader(state)
                Panel(title = MobileDisplayText.CURRENT_WORK_TITLE, modifier = Modifier.fillMaxWidth()) {
                    if (state.currentWork.isEmpty()) {
                        MobileEmptyLine(MobileDisplayText.NO_CURRENT_WORK)
                    } else {
                        state.currentWork.forEach { item -> MobileWorkRow(item) }
                    }
                }
                Panel(title = MobileDisplayText.ATTENTION_QUEUE_TITLE, modifier = Modifier.fillMaxWidth()) {
                    if (state.attentionQueue.isEmpty()) {
                        MobileEmptyLine(MobileDisplayText.NO_ITEMS_NEED_ATTENTION)
                    } else {
                        state.attentionQueue.forEach { item -> MobileAttentionRow(item) }
                    }
                }
                Panel(title = MobileDisplayText.RECENT_EVENTS_TITLE, modifier = Modifier.fillMaxWidth()) {
                    if (state.recentEvents.isEmpty()) {
                        MobileEmptyRow(MobileDisplayText.NO_RECENT_ACCEPTED_EVENTS)
                    } else {
                        state.recentEvents.forEach { event -> MobileEventRow(event) }
                    }
                }
                Panel(title = MobileDisplayText.TIMELINE_TITLE, modifier = Modifier.fillMaxWidth()) {
                    if (state.timeline.isEmpty()) {
                        MobileEmptyRow(MobileDisplayText.NO_TIMELINE_ENTRIES)
                    } else {
                        if (state.timelineStatusMarkers.isNotEmpty()) {
                            Text(
                                text = MobileDisplayText.timelineStatus(state.timelineStatusMarkers),
                                color = AgentDeskTheme.colors.textMuted,
                                fontFamily = AgentDeskTheme.typography.monoFamily,
                                fontSize = 10.sp,
                            )
                        }
                        val detailsByEventId = state.evidenceDetails.associateBy { detail -> detail.eventId }
                        var previousWindow: String? = null
                        state.timeline.forEach { entry ->
                            if (entry.timeWindow != previousWindow) {
                                previousWindow = entry.timeWindow
                                Text(
                                    text = entry.timeWindow,
                                    color = AgentDeskTheme.colors.textMuted,
                                    fontFamily = AgentDeskTheme.typography.monoFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            MobileTimelineRow(
                                entry = entry,
                                detail = detailsByEventId[entry.eventId],
                            )
                        }
                    }
                }
                if (state.projectionWarnings.isNotEmpty()) {
                    Panel(title = MobileDisplayText.PROJECTION_WARNINGS_TITLE, modifier = Modifier.fillMaxWidth()) {
                        state.projectionWarnings.forEach { warning -> MobileWarningRow(warning) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileHeader(state: MobileOperatorState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = MobileDisplayText.APP_TITLE,
            color = AgentDeskTheme.colors.textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = MobileDisplayText.summary(
                currentWorkCount = state.currentWork.size,
                attentionCount = state.attentionQueue.size,
            ),
            color = AgentDeskTheme.colors.textMuted,
            fontFamily = AgentDeskTheme.typography.monoFamily,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun MobileAttentionRow(item: MobileAttentionItem) {
    MobileWorkRow(
        item = item.workItem,
        footer = item.stale?.let(MobileDisplayText::staleAttention),
    )
}

@Composable
private fun MobileWorkRow(
    item: MobileWorkItem,
    footer: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(AgentDeskTheme.colors.row)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = item.id,
                color = AgentDeskTheme.colors.textMuted,
                fontFamily = AgentDeskTheme.typography.monoFamily,
                fontSize = 11.sp,
            )
            Text(
                text = item.status.label,
                color = AgentDeskTheme.statusRole(item.status.tone).text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = item.title,
            color = AgentDeskTheme.colors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        item.summary?.let { summary ->
            Text(
                text = summary,
                color = AgentDeskTheme.colors.textSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
        if (item.evidenceReferences.isNotEmpty()) {
            Text(
                text = MobileDisplayText.evidenceReferences(item.evidenceReferences),
                color = AgentDeskTheme.colors.textMuted,
                fontFamily = AgentDeskTheme.typography.monoFamily,
                fontSize = 10.sp,
            )
        }
        footer?.let {
            Text(
                text = it,
                color = AgentDeskTheme.statusRole(StatusTone.Attention).text,
                fontFamily = AgentDeskTheme.typography.monoFamily,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun MobileEventRow(event: MobileEventLine) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(AgentDeskTheme.colors.row)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = event.occurredAt,
                color = AgentDeskTheme.colors.textMuted,
                fontFamily = AgentDeskTheme.typography.monoFamily,
                fontSize = 10.sp,
            )
            Text(
                text = event.type,
                color = AgentDeskTheme.colors.accent,
                fontFamily = AgentDeskTheme.typography.monoFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = event.workItemId,
            color = AgentDeskTheme.colors.textMuted,
            fontFamily = AgentDeskTheme.typography.monoFamily,
            fontSize = 11.sp,
        )
        Text(
            text = event.detail,
            color = AgentDeskTheme.colors.textSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
        if (event.evidenceReferences.isNotEmpty()) {
            Text(
                text = MobileDisplayText.evidenceReferences(event.evidenceReferences),
                color = AgentDeskTheme.colors.textMuted,
                fontFamily = AgentDeskTheme.typography.monoFamily,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun MobileTimelineRow(
    entry: MobileTimelineEntry,
    detail: MobileEvidenceDetail?,
) {
    var expanded by remember { mutableStateOf(false) }
    // The whole row is a disclosure toggle: dense fields stack vertically and
    // wrap, never sharing a SpaceBetween row that could clip on narrow screens.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(AgentDeskTheme.colors.row)
            .clickable { expanded = !expanded }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = entry.occurredAt,
            color = AgentDeskTheme.colors.textMuted,
            fontFamily = AgentDeskTheme.typography.monoFamily,
            fontSize = 10.sp,
        )
        Text(
            text = entry.type,
            color = AgentDeskTheme.colors.accent,
            fontFamily = AgentDeskTheme.typography.monoFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = entry.workItemId,
            color = AgentDeskTheme.colors.textMuted,
            fontFamily = AgentDeskTheme.typography.monoFamily,
            fontSize = 11.sp,
        )
        Text(
            text = entry.stateLabel,
            color = timelineStateColor(entry.stateLabel),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = entry.summary,
            color = AgentDeskTheme.colors.textSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
        entry.completionSummary?.let { completion ->
            Text(
                text = completion,
                color = completionSummaryColor(completion),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        if (entry.evidenceReferences.isNotEmpty()) {
            Text(
                text = MobileDisplayText.evidenceReferences(entry.evidenceReferences),
                color = AgentDeskTheme.colors.textMuted,
                fontFamily = AgentDeskTheme.typography.monoFamily,
                fontSize = 10.sp,
            )
        }
        if (detail != null) {
            Text(
                text = if (expanded) {
                    MobileDisplayText.DETAILS_DISCLOSURE_EXPANDED
                } else {
                    MobileDisplayText.DETAILS_DISCLOSURE_COLLAPSED
                },
                color = AgentDeskTheme.colors.textMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
            if (expanded) {
                MobileDisplayText.evidenceDetailRows(detail).forEach { row ->
                    Text(
                        text = row,
                        color = AgentDeskTheme.colors.textSecondary,
                        fontFamily = AgentDeskTheme.typography.monoFamily,
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun timelineStateColor(stateLabel: String): Color = when (stateLabel) {
    "Blocked" -> AgentDeskTheme.statusRole(StatusTone.Blocked).text
    "Failed" -> AgentDeskTheme.statusRole(StatusTone.Failure).text
    "Completed" -> AgentDeskTheme.statusRole(StatusTone.Success).text
    "Stale", "Not done", "Partial" -> AgentDeskTheme.statusRole(StatusTone.Attention).text
    else -> AgentDeskTheme.colors.textMuted
}

@Composable
private fun completionSummaryColor(completionSummary: String): Color = when (completionSummary) {
    "Successful outcome" -> AgentDeskTheme.statusRole(StatusTone.Success).text
    "Failed outcome" -> AgentDeskTheme.statusRole(StatusTone.Failure).text
    else -> AgentDeskTheme.colors.textMuted
}

@Composable
private fun MobileWarningRow(warning: MobileProjectionWarning) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = warning.eventId,
            color = AgentDeskTheme.colors.textMuted,
            fontFamily = AgentDeskTheme.typography.monoFamily,
            fontSize = 11.sp,
        )
        Text(
            text = warning.reason,
            color = AgentDeskTheme.colors.textSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun MobileEmptyRow(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(AgentDeskTheme.colors.row)
            .padding(12.dp),
        color = AgentDeskTheme.colors.textMuted,
        fontSize = 12.sp,
    )
}

@Composable
private fun MobileEmptyLine(text: String) {
    Text(
        text = text,
        color = AgentDeskTheme.colors.textMuted,
        fontSize = 12.sp,
    )
}
