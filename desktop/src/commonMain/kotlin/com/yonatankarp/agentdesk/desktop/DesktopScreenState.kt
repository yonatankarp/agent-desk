package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.app.config.AgentDeskMode
import com.yonatankarp.agentdesk.app.config.AgentDeskRuntimeConfig
import com.yonatankarp.agentdesk.app.operator.OperatorState

sealed interface DesktopScreenState {
    val modeLabel: String

    data object Loading : DesktopScreenState {
        override val modeLabel: String = "Loading state"
    }

    data class Ready(
        val state: OperatorState,
        override val modeLabel: String,
    ) : DesktopScreenState

    data class Error(
        val message: String,
    ) : DesktopScreenState {
        override val modeLabel: String = "Invalid configuration"
    }
}

class DesktopStateResolver(
    private val loadState: (AgentDeskRuntimeConfig) -> OperatorState,
) {
    fun resolve(config: AgentDeskRuntimeConfig): DesktopScreenState = try {
        DesktopScreenState.Ready(
            state = loadState(config),
            modeLabel = config.mode.modeLabel(),
        )
    } catch (error: RuntimeException) {
        DesktopScreenState.Error(error.publicSafeDesktopMessage())
    }

    private fun AgentDeskMode.modeLabel(): String = when (this) {
        AgentDeskMode.Sample -> "Sample state"
        AgentDeskMode.StoredEvents -> "Loaded state"
    }

    private fun RuntimeException.publicSafeDesktopMessage(): String {
        val text = message.orEmpty()
        return when {
            text.contains("mode", ignoreCase = true) -> text
            text.contains("source", ignoreCase = true) -> text
            text.contains("eventStoreLocation", ignoreCase = true) -> text
            text.startsWith("Configured event store") -> text
            else -> "Configured operator state could not be loaded."
        }
    }
}
