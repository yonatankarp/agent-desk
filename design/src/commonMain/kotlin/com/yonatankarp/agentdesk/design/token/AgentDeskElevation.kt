package com.yonatankarp.agentdesk.design.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AgentDeskElevation(
    val panelTonal: Dp = 1.dp,
    val panelShadow: Dp = 2.dp,
) {
    companion object {
        val Default = AgentDeskElevation()
    }
}
