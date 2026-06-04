package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.app.config.AgentDeskMode
import com.yonatankarp.agentdesk.app.config.AgentDeskRuntimeConfig

object RuntimeConfiguredOperatorStateLoader {
    fun load(config: AgentDeskRuntimeConfig): OperatorState = when (config.mode) {
        AgentDeskMode.Sample -> SampleOperatorState.current()
        AgentDeskMode.StoredEvents -> loadStoredEvents(config)
    }

    private fun loadStoredEvents(config: AgentDeskRuntimeConfig): OperatorState = try {
        val events = RuntimeConfiguredWorkEventLoader.load(config)
        OperatorStateProjector.project(events)
    } catch (error: OperatorStateProjectionException) {
        throw RuntimeConfiguredOperatorStateLoadException(error.message ?: "Configured event store could not be projected.", error)
    }
}
