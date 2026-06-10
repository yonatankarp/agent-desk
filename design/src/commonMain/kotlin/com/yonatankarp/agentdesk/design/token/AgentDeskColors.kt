package com.yonatankarp.agentdesk.design.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AgentDeskColors(
    val background: Color,
    val panel: Color,
    val line: Color,
    val row: Color,
    val rowLine: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentAlt: Color,
) {
    companion object {
        val Light = AgentDeskColors(
            background = Color(0xFFF5F8FC),
            panel = Color(0xFFFFFFFF),
            line = Color(0xFFE4EAF1),
            row = Color(0xFFF8FAFD),
            rowLine = Color(0xFFEDF1F6),
            textPrimary = Color(0xFF172230),
            textSecondary = Color(0xFF44515F),
            // Pinned by AgentDeskContrastTest for AA on background/panel/row.
            textMuted = Color(0xFF5E6470),
            accent = Color(0xFF0E7C8B),
            accentAlt = Color(0xFF2563EB),
        )

        val Dark = AgentDeskColors(
            background = Color(0xFF0F1823),
            panel = Color(0xFF17222F),
            line = Color(0xFF26364A),
            row = Color(0xFF1C2937),
            rowLine = Color(0xFF26364A),
            textPrimary = Color(0xFFE7EFF7),
            textSecondary = Color(0xFFC2CEDC),
            // Pinned by AgentDeskContrastTest for AA on background/panel/row.
            textMuted = Color(0xFF93A2B5),
            accent = Color(0xFF2DD4BF),
            accentAlt = Color(0xFF60A5FA),
        )
    }
}
