@file:Suppress("FunctionName")

package com.yonatankarp.agentdesk.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.yonatankarp.agentdesk.design.theme.AgentDeskTheme
import com.yonatankarp.agentdesk.design.theme.ThemeMode

@Composable
fun ThemeModeControl(
    mode: ThemeMode,
    onCycle: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AgentDeskTheme.colors
    val spacing = AgentDeskTheme.spacing
    val label = when (mode) {
        ThemeMode.System -> "Auto"
        ThemeMode.Light -> "Light"
        ThemeMode.Dark -> "Dark"
    }
    Text(
        text = "Theme: $label",
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(colors.row)
            .border(spacing.lineWidth, colors.line, RoundedCornerShape(50))
            .clickable { onCycle(mode.next()) }
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        color = colors.textSecondary,
        style = AgentDeskTheme.typography.label,
    )
}
