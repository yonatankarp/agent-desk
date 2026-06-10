@file:Suppress("FunctionName")

package com.yonatankarp.agentdesk.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(spacing.panelRadius))
            .background(colors.panel)
            .border(1.dp, colors.line, RoundedCornerShape(spacing.panelRadius)),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.md)) {
            Text(
                text = title,
                color = titleColor ?: colors.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
            )
            if (count != null) {
                Spacer(Modifier.weight(1f))
                Text(text = count.toString(), color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
