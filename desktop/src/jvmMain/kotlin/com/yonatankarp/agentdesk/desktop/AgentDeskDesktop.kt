package com.yonatankarp.agentdesk.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main(args: Array<String>) = application {
    val screenState = try {
        DesktopRuntimeStateProvider.load(args)
    } catch (error: RuntimeException) {
        DesktopScreenState.Error(error.message ?: "Configured operator state could not be loaded.")
    }

    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(width = 1180.dp, height = 760.dp),
        title = "Agent Desk",
    ) {
        AgentDeskApp(screenState)
    }
}
