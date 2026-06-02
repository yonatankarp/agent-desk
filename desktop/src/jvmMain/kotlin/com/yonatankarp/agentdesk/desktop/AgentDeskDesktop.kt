package com.yonatankarp.agentdesk.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(width = 1180.dp, height = 760.dp),
        title = "Agent Desk",
    ) {
        AgentDeskApp(SampleDesktopState.current())
    }
}
