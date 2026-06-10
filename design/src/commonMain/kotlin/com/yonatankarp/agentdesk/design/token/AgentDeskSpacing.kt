package com.yonatankarp.agentdesk.design.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AgentDeskSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 18.dp,
    val xl: Dp = 24.dp,
    val panelRadius: Dp = 14.dp,
    val rowRadius: Dp = 10.dp,
    val railWidth: Dp = 3.dp,
) {
    companion object {
        val Default = AgentDeskSpacing()
    }
}
