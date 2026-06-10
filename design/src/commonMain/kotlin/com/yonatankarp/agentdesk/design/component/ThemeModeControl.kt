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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yonatankarp.agentdesk.design.theme.AgentDeskTheme
import com.yonatankarp.agentdesk.design.theme.ThemeMode

@Composable
fun ThemeModeControl(
    mode: ThemeMode,
    onCycle: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AgentDeskTheme.colors
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
            .border(1.dp, colors.line, RoundedCornerShape(50))
            .clickable { onCycle(mode.next()) }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = colors.textSecondary,
        fontSize = 12.sp,
    )
}
