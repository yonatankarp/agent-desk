@file:Suppress("FunctionName")

package com.yonatankarp.agentdesk.design.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yonatankarp.agentdesk.design.theme.AgentDeskTheme

@Composable
fun Panel(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    titleColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val colors = AgentDeskTheme.colors
    val spacing = AgentDeskTheme.spacing
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(spacing.panelRadius))
            .border(1.dp, colors.line, RoundedCornerShape(spacing.panelRadius)),
        color = colors.panel,
        shape = RoundedCornerShape(spacing.panelRadius),
        tonalElevation = AgentDeskTheme.elevation.panelTonal,
        shadowElevation = AgentDeskTheme.elevation.panelShadow,
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.md)) {
                Text(
                    text = title,
                    color = titleColor ?: colors.textMuted,
                    style = AgentDeskTheme.typography.section,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                )
                if (count != null) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = count.toString(),
                        color = colors.textSecondary,
                        style = AgentDeskTheme.typography.section,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Column(
                modifier = Modifier.padding(start = spacing.md, end = spacing.md, bottom = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                content()
            }
        }
    }
}
