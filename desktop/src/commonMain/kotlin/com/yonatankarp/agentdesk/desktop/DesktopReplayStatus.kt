package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.app.operator.ReplayStatusPresenter

object DesktopReplayStatus {
    fun rows(screenState: DesktopScreenState): List<String> = when (screenState) {
        DesktopScreenState.Loading -> listOf("Replay status: loading operator state.")
        is DesktopScreenState.Error -> ReplayStatusPresenter.failureRows(screenState.message)
        is DesktopScreenState.Ready -> ReplayStatusPresenter.readyRows(screenState.state, screenState.modeLabel)
    }
}
