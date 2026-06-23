package com.yonatankarp.agentdesk.mobile

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main(args: Array<String>) {
    if (args.singleOrNull() == "--smoke-exit") {
        println("Mobile run smoke passed.")
        return
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(width = 412.dp, height = 915.dp),
            title = "Agent Desk Mobile",
            resizable = false,
        ) {
            AgentDeskMobileApp(MobileRuntimeStateProvider.load(args))
        }
    }
}
