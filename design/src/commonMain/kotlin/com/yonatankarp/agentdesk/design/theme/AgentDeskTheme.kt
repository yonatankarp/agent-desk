package com.yonatankarp.agentdesk.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.yonatankarp.agentdesk.app.operator.StatusTone
import com.yonatankarp.agentdesk.design.token.AgentDeskColors
import com.yonatankarp.agentdesk.design.token.AgentDeskSpacing
import com.yonatankarp.agentdesk.design.token.AgentDeskTypography
import com.yonatankarp.agentdesk.design.token.StatusColors
import com.yonatankarp.agentdesk.design.token.StatusRole

val LocalAgentDeskColors = staticCompositionLocalOf { AgentDeskColors.Light }
val LocalStatusColors = staticCompositionLocalOf { StatusColors.Light }
val LocalSpacing = staticCompositionLocalOf { AgentDeskSpacing.Default }
val LocalAgentDeskTypography = staticCompositionLocalOf<AgentDeskTypography?> { null }

object AgentDeskTheme {
    val colors: AgentDeskColors
        @Composable get() = LocalAgentDeskColors.current
    val status: StatusColors
        @Composable get() = LocalStatusColors.current
    val spacing: AgentDeskSpacing
        @Composable get() = LocalSpacing.current
    val typography: AgentDeskTypography
        @Composable get() = LocalAgentDeskTypography.current ?: AgentDeskTypography()

    @Composable
    fun statusRole(tone: StatusTone): StatusRole = LocalStatusColors.current.forTone(tone)
}

@Composable
fun AgentDeskTheme(
    mode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val dark = mode.resolvesToDark(isSystemInDarkTheme())
    val colors = if (dark) AgentDeskColors.Dark else AgentDeskColors.Light
    val status = if (dark) StatusColors.Dark else StatusColors.Light
    val typography = AgentDeskTypography()

    val material = if (dark) {
        darkColorScheme(
            background = colors.background,
            surface = colors.panel,
            primary = colors.accent,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary,
        )
    } else {
        lightColorScheme(
            background = colors.background,
            surface = colors.panel,
            primary = colors.accent,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary,
        )
    }

    CompositionLocalProvider(
        LocalAgentDeskColors provides colors,
        LocalStatusColors provides status,
        LocalSpacing provides AgentDeskSpacing.Default,
        LocalAgentDeskTypography provides typography,
    ) {
        MaterialTheme(colorScheme = material, content = content)
    }
}
