@file:Suppress("FunctionName")

package com.yonatankarp.agentdesk.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yonatankarp.agentdesk.app.operator.StatusTone
import com.yonatankarp.agentdesk.design.theme.AgentDeskTheme
import com.yonatankarp.agentdesk.design.token.AgentDeskTypography

@Composable
fun ActionRow(
    title: String,
    tone: StatusTone,
    statusLabel: String,
    modifier: Modifier = Modifier,
    id: String? = null,
) {
    val colors = AgentDeskTheme.colors
    val spacing = AgentDeskTheme.spacing
    val rail = AgentDeskTheme.statusRole(tone).rail
    val railPx = with(LocalDensity.current) { spacing.railWidth.toPx() }
    val mono = AgentDeskTypography().monoFamily

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(spacing.rowRadius))
            .background(colors.row)
            .drawBehind {
                drawRect(color = rail, topLeft = Offset.Zero, size = Size(railPx, size.height))
            }
            .padding(start = spacing.md + spacing.xs, top = spacing.md, bottom = spacing.md, end = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        if (id != null) {
            Text(text = id, color = colors.textMuted, fontFamily = mono, fontSize = 11.sp)
        }
        Text(text = title, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        StatusPill(label = statusLabel, tone = tone)
    }
}
