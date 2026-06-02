@file:Suppress("FunctionName")

package com.yonatankarp.agentdesk.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yonatankarp.agentdesk.core.WorkItem

@Composable
@Preview
fun previewAgentDeskApp() {
    AgentDeskApp(SampleDesktopState.current())
}

@Composable
fun AgentDeskApp(state: DesktopOperatorState) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            background = Palette.Background,
            surface = Palette.Surface,
            primary = Palette.Accent,
            onBackground = Palette.TextPrimary,
            onSurface = Palette.TextPrimary,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Palette.Background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Header(state)

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.35f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        SectionPanel(
                            title = "Current work",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) {
                            WorkList(state.workItems)
                        }

                        SectionPanel(
                            title = "Recent events",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) {
                            EventTimeline(DesktopStatePresenter.eventLines(state))
                        }
                    }

                    SectionPanel(
                        title = "Attention queue",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        AttentionList(DesktopStatePresenter.attentionItems(state))
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(state: DesktopOperatorState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Agent Desk",
                color = Palette.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Local operator console",
                color = Palette.TextMuted,
                fontSize = 14.sp,
            )
            Text(
                text = "Sample state",
                color = Palette.TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
        }

        val attentionCount = DesktopStatePresenter.attentionItems(state).size
        Text(
            text = "${DesktopStatePresenter.activeCount(state)} active / $attentionCount attention",
            color = Palette.TextMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun SectionPanel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, RoundedCornerShape(6.dp)),
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            color = Palette.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        HorizontalDivider(color = Palette.Line)
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun WorkList(items: List<WorkItem>) {
    if (items.isEmpty()) {
        EmptyLine("No current work")
        return
    }

    items.forEach { item ->
        WorkRow(item)
    }
}

@Composable
private fun AttentionList(items: List<WorkItem>) {
    if (items.isEmpty()) {
        EmptyLine("No items need a decision")
        return
    }

    items.forEach { item ->
        WorkRow(item)
    }
}

@Composable
private fun WorkRow(item: WorkItem) {
    val status = DesktopStatePresenter.presentationFor(item.status)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Palette.Row)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusDot(status.tone)
            Text(
                text = item.id.toString(),
                color = Palette.TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = status.label,
                color = colorFor(status.tone),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Text(
            text = item.title.toString(),
            color = Palette.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
        item.summary?.let { summary ->
            Text(
                text = summary.toString(),
                color = Palette.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun EventTimeline(lines: List<EventLine>) {
    if (lines.isEmpty()) {
        EmptyLine("No recent events")
        return
    }

    lines.forEach { line ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = line.type,
                    color = Palette.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = line.occurredAt,
                    color = Palette.TextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                )
            }
            Text(
                text = "${line.workItemId} from ${line.source}",
                color = Palette.TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
            Text(
                text = line.detail,
                color = Palette.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun EmptyLine(text: String) {
    Text(
        text = text,
        color = Palette.TextMuted,
        fontSize = 13.sp,
    )
}

@Composable
private fun StatusDot(tone: StatusTone) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(colorFor(tone)),
    )
}

private fun colorFor(tone: StatusTone): Color = when (tone) {
    StatusTone.Neutral -> Palette.TextMuted
    StatusTone.Active -> Palette.Accent
    StatusTone.Attention -> Palette.Attention
    StatusTone.Blocked -> Palette.Blocked
    StatusTone.Success -> Palette.Success
    StatusTone.Failure -> Palette.Failure
}

private object Palette {
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
