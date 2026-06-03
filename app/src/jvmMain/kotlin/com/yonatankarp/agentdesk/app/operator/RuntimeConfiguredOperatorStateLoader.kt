package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.app.config.AgentDeskMode
import com.yonatankarp.agentdesk.app.config.AgentDeskRuntimeConfig
import com.yonatankarp.agentdesk.app.persistence.LocalFileWorkEventRepository
import com.yonatankarp.agentdesk.app.persistence.WorkEventStoreException
import java.nio.file.InvalidPathException
import java.nio.file.Path

class RuntimeConfiguredOperatorStateLoader {
    fun load(config: AgentDeskRuntimeConfig): OperatorState = when (config.mode) {
        AgentDeskMode.Sample -> SampleOperatorState.current()
        AgentDeskMode.StoredEvents -> loadStoredEvents(config)
    }

    private fun loadStoredEvents(config: AgentDeskRuntimeConfig): OperatorState {
        val location = config.eventStoreLocation
            ?: throw OperatorStateLoadException("stored event mode requires eventStoreLocation")
        val storePath =
            try {
                Path.of(location.value)
            } catch (exception: InvalidPathException) {
                throw OperatorStateLoadException("Configured event store could not be read.", exception)
            }

        val events =
            try {
                LocalFileWorkEventRepository(storePath).readAll()
            } catch (exception: WorkEventStoreException) {
                throw OperatorStateLoadException(
                    exception.message ?: "Configured event store could not be read.",
                    exception,
                )
            } catch (exception: SecurityException) {
                throw OperatorStateLoadException("Configured event store could not be read.", exception)
            }

        return OperatorStateProjector.project(events)
    }
}
