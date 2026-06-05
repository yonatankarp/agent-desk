@file:Suppress("FunctionName")

package com.yonatankarp.agentdesk.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yonatankarp.agentdesk.app.operator.StatusTone
import com.yonatankarp.agentdesk.app.operator.mobile.MobileAttentionItem
import com.yonatankarp.agentdesk.app.operator.mobile.MobileEventLine
import com.yonatankarp.agentdesk.app.operator.mobile.MobileOperatorState
import com.yonatankarp.agentdesk.app.operator.mobile.MobileOperatorStateContract
import com.yonatankarp.agentdesk.app.operator.mobile.MobileProjectionWarning
import com.yonatankarp.agentdesk.app.operator.mobile.MobileWorkItem

@Composable
fun AgentDeskMobileApp(state: MobileOperatorState = MobileOperatorStateContract.sample()) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            background = MobilePalette.Background,
            surface = MobilePalette.Surface,
            primary = MobilePalette.Accent,
            onBackground = MobilePalette.TextPrimary,
            onSurface = MobilePalette.TextPrimary,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MobilePalette.Background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                MobileHeader(state)
                MobileSection(title = MobileDisplayText.CURRENT_WORK_TITLE) {
                    if (state.currentWork.isEmpty()) {
                        MobileEmptyLine(MobileDisplayText.NO_CURRENT_WORK)
                    } else {
                        state.currentWork.forEach { item -> MobileWorkRow(item) }
                    }
                }
                MobileSection(title = MobileDisplayText.ATTENTION_QUEUE_TITLE) {
                    if (state.attentionQueue.isEmpty()) {
                        MobileEmptyLine(MobileDisplayText.NO_ITEMS_NEED_ATTENTION)
                    } else {
                        state.attentionQueue.forEach { item -> MobileAttentionRow(item) }
                    }
                }
                MobileSection(title = MobileDisplayText.RECENT_EVENTS_TITLE) {
                    if (state.recentEvents.isEmpty()) {
                        MobileEmptyRow(MobileDisplayText.NO_RECENT_ACCEPTED_EVENTS)
                    } else {
                        state.recentEvents.forEach { event -> MobileEventRow(event) }
                    }
                }
                if (state.projectionWarnings.isNotEmpty()) {
                    MobileSection(title = MobileDisplayText.PROJECTION_WARNINGS_TITLE) {
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
            color = MobilePalette.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = MobileDisplayText.summary(
                currentWorkCount = state.currentWork.size,
                attentionCount = state.attentionQueue.size,
            ),
            color = MobilePalette.TextMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun MobileSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MobilePalette.Surface)
            .border(1.dp, MobilePalette.Line, RoundedCornerShape(6.dp)),
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            color = MobilePalette.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        HorizontalDivider(color = MobilePalette.Line)
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            content()
        }
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
            .background(MobilePalette.Row)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = item.id,
                color = MobilePalette.TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
            Text(
                text = item.status.label,
                color = colorFor(item.status.tone),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = item.title,
            color = MobilePalette.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        item.summary?.let { summary ->
            Text(
                text = summary,
                color = MobilePalette.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
        if (item.evidenceReferences.isNotEmpty()) {
            Text(
                text = MobileDisplayText.evidenceReferences(item.evidenceReferences),
                color = MobilePalette.TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
        }
        footer?.let {
            Text(
                text = it,
                color = MobilePalette.Attention,
                fontFamily = FontFamily.Monospace,
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
            .background(MobilePalette.Row)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = event.occurredAt,
                color = MobilePalette.TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
            Text(
                text = event.type,
                color = MobilePalette.Accent,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = event.workItemId,
            color = MobilePalette.TextMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
        Text(
            text = event.detail,
            color = MobilePalette.TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
        if (event.evidenceReferences.isNotEmpty()) {
            Text(
                text = MobileDisplayText.evidenceReferences(event.evidenceReferences),
                color = MobilePalette.TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun MobileWarningRow(warning: MobileProjectionWarning) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = warning.eventId,
            color = MobilePalette.TextMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
        Text(
            text = warning.reason,
            color = MobilePalette.TextSecondary,
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
            .background(MobilePalette.Row)
            .padding(12.dp),
        color = MobilePalette.TextMuted,
        fontSize = 12.sp,
    )
}

@Composable
private fun MobileEmptyLine(text: String) {
    Text(
        text = text,
        color = MobilePalette.TextMuted,
        fontSize = 12.sp,
    )
}

internal fun colorFor(tone: StatusTone): Color = when (tone) {
    StatusTone.Neutral -> MobilePalette.TextMuted
    StatusTone.Active -> MobilePalette.Accent
    StatusTone.Attention -> MobilePalette.Attention
    StatusTone.Blocked -> MobilePalette.Blocked
    StatusTone.Success -> MobilePalette.Success
    StatusTone.Failure -> MobilePalette.Failure
}

internal object MobilePalette {
    val Background = Color(0xFFF7F8FA)
    val Surface = Color(0xFFFFFFFF)
    val Row = Color(0xFFF4F6F8)
    val Line = Color(0xFFE1E5EA)
    val TextPrimary = Color(0xFF1D252D)
    val TextSecondary = Color(0xFF4B5563)
    val TextMuted = Color(0xFF6B7280)
    val Accent = Color(0xFF0F766E)
    val Attention = Color(0xFFA16207)
    val Blocked = Color(0xFFB45309)
    val Success = Color(0xFF15803D)
    val Failure = Color(0xFFB91C1C)
}
