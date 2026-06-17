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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.yonatankarp.agentdesk.design.component.StatusPill
import com.yonatankarp.agentdesk.design.component.ThemeModeControl
import com.yonatankarp.agentdesk.design.theme.AgentDeskTheme
import com.yonatankarp.agentdesk.design.theme.ThemeMode

@Composable
fun AgentDeskMobileApp(state: MobileOperatorState = MobileOperatorStateContract.sample()) {
    var mode by remember { mutableStateOf(ThemeMode.System) }
    AgentDeskTheme(mode = mode) {
        val spacing = AgentDeskTheme.spacing
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AgentDeskTheme.colors.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.lg, vertical = spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                MobileHeader(state, mode) { mode = it }
                Panel(title = MobileDisplayText.CURRENT_WORK_TITLE, modifier = Modifier.fillMaxWidth()) {
                    if (state.currentWork.isEmpty()) {
                        MobileEmptyRow(MobileDisplayText.NO_CURRENT_WORK)
                    } else {
                        state.currentWork.forEach { item -> MobileWorkRow(item) }
                    }
                }
                Panel(title = MobileDisplayText.ATTENTION_QUEUE_TITLE, modifier = Modifier.fillMaxWidth()) {
                    if (state.attentionQueue.isEmpty()) {
                        MobileEmptyRow(MobileDisplayText.NO_ITEMS_NEED_ATTENTION)
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
                                style = AgentDeskTheme.typography.mono,
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
                                    style = AgentDeskTheme.typography.mono,
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
private fun MobileHeader(
    state: MobileOperatorState,
    mode: ThemeMode,
    onCycle: (ThemeMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AgentDeskTheme.spacing.sm)) {
        Text(
            text = MobileDisplayText.APP_TITLE,
            color = AgentDeskTheme.colors.textPrimary,
            style = AgentDeskTheme.typography.display,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = MobileDisplayText.summary(
                currentWorkCount = state.currentWork.size,
                attentionCount = state.attentionQueue.size,
            ),
            color = AgentDeskTheme.colors.textMuted,
            style = AgentDeskTheme.typography.mono,
        )
        ThemeModeControl(mode = mode, onCycle = onCycle)
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
    val spacing = AgentDeskTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(spacing.rowRadius))
            .background(AgentDeskTheme.colors.row)
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.id,
                color = AgentDeskTheme.colors.textMuted,
                style = AgentDeskTheme.typography.mono,
            )
            StatusPill(label = item.status.label, tone = item.status.tone)
        }
        Text(
            text = item.title,
            color = AgentDeskTheme.colors.textPrimary,
            style = AgentDeskTheme.typography.rowTitle,
            fontWeight = FontWeight.Medium,
        )
        item.summary?.let { summary ->
            Text(
                text = summary,
                color = AgentDeskTheme.colors.textSecondary,
                style = AgentDeskTheme.typography.body,
            )
        }
        if (item.evidenceReferences.isNotEmpty()) {
            Text(
                text = MobileDisplayText.evidenceReferences(item.evidenceReferences),
                color = AgentDeskTheme.colors.textMuted,
                style = AgentDeskTheme.typography.mono,
            )
        }
        footer?.let {
            Text(
                text = it,
                color = AgentDeskTheme.statusRole(StatusTone.Attention).text,
                style = AgentDeskTheme.typography.mono,
            )
        }
    }
}

@Composable
private fun MobileEventRow(event: MobileEventLine) {
    val spacing = AgentDeskTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(spacing.rowRadius))
            .background(AgentDeskTheme.colors.row)
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = event.occurredAt,
                color = AgentDeskTheme.colors.textMuted,
                style = AgentDeskTheme.typography.mono,
            )
            Text(
                text = event.type,
                color = AgentDeskTheme.colors.accent,
                style = AgentDeskTheme.typography.mono,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = event.workItemId,
            color = AgentDeskTheme.colors.textMuted,
            style = AgentDeskTheme.typography.mono,
        )
        Text(
            text = event.detail,
            color = AgentDeskTheme.colors.textSecondary,
            style = AgentDeskTheme.typography.body,
        )
        if (event.evidenceReferences.isNotEmpty()) {
            Text(
                text = MobileDisplayText.evidenceReferences(event.evidenceReferences),
                color = AgentDeskTheme.colors.textMuted,
                style = AgentDeskTheme.typography.mono,
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
    val spacing = AgentDeskTheme.spacing
    // The whole row is a disclosure toggle: dense fields stack vertically and
    // wrap, never sharing a SpaceBetween row that could clip on narrow screens.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(spacing.rowRadius))
            .background(AgentDeskTheme.colors.row)
            .clickable { expanded = !expanded }
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text(
            text = entry.occurredAt,
            color = AgentDeskTheme.colors.textMuted,
            style = AgentDeskTheme.typography.mono,
        )
        Text(
            text = entry.type,
            color = AgentDeskTheme.colors.accent,
            style = AgentDeskTheme.typography.mono,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = MobileDisplayText.timelineSource(entry),
            color = AgentDeskTheme.colors.textMuted,
            style = AgentDeskTheme.typography.mono,
        )
        Text(
            text = entry.stateLabel,
            color = timelineStateColor(entry.stateLabel),
            style = AgentDeskTheme.typography.label,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = entry.summary,
            color = AgentDeskTheme.colors.textSecondary,
            style = AgentDeskTheme.typography.body,
        )
        entry.completionSummary?.let { completion ->
            Text(
                text = completion,
                color = completionSummaryColor(completion),
                style = AgentDeskTheme.typography.label,
                fontWeight = FontWeight.Medium,
            )
        }
        if (entry.evidenceReferences.isNotEmpty()) {
            Text(
                text = MobileDisplayText.evidenceReferences(entry.evidenceReferences),
                color = AgentDeskTheme.colors.textMuted,
                style = AgentDeskTheme.typography.mono,
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
                style = AgentDeskTheme.typography.caption,
                fontWeight = FontWeight.Medium,
            )
            if (expanded) {
                MobileDisplayText.evidenceDetailRows(detail).forEach { row ->
                    Text(
                        text = row,
                        color = AgentDeskTheme.colors.textSecondary,
                        style = AgentDeskTheme.typography.mono,
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
    Column(verticalArrangement = Arrangement.spacedBy(AgentDeskTheme.spacing.xs)) {
        Text(
            text = warning.eventId,
            color = AgentDeskTheme.colors.textMuted,
            style = AgentDeskTheme.typography.mono,
        )
        Text(
            text = warning.reason,
            color = AgentDeskTheme.colors.textSecondary,
            style = AgentDeskTheme.typography.body,
        )
    }
}

@Composable
private fun MobileEmptyRow(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(MOBILE_EMPTY_ROW_TEST_TAG)
            .clip(RoundedCornerShape(AgentDeskTheme.spacing.rowRadius))
            .background(AgentDeskTheme.colors.row)
            .padding(AgentDeskTheme.spacing.md),
        color = AgentDeskTheme.colors.textMuted,
        style = AgentDeskTheme.typography.body,
    )
}

internal const val MOBILE_EMPTY_ROW_TEST_TAG = "mobile-empty-row"
