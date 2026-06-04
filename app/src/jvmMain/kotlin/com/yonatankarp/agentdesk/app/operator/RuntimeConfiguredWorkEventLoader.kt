package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.app.config.AgentDeskMode
import com.yonatankarp.agentdesk.app.config.AgentDeskRuntimeConfig
import com.yonatankarp.agentdesk.app.persistence.LocalFileWorkEventRepository
import com.yonatankarp.agentdesk.app.persistence.PublicSafeWorkEventStoreMessage
import com.yonatankarp.agentdesk.app.persistence.WorkEventStoreException
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import java.nio.file.InvalidPathException
import java.nio.file.Path

object RuntimeConfiguredWorkEventLoader {
    fun load(config: AgentDeskRuntimeConfig): List<WorkEvent> = when (config.mode) {
        AgentDeskMode.Sample -> SampleOperatorState.current().events
        AgentDeskMode.StoredEvents -> loadStoredEvents(config)
    }

    private fun loadStoredEvents(config: AgentDeskRuntimeConfig): List<WorkEvent> {
        val location = config.eventStoreLocation
            ?: throw RuntimeConfiguredOperatorStateLoadException("Configured event store could not be read.")

        return try {
            LocalFileWorkEventRepository(Path.of(location.value)).readAll()
        } catch (error: InvalidPathException) {
            throw RuntimeConfiguredOperatorStateLoadException("Configured event store could not be read.", error)
        } catch (error: SecurityException) {
            throw RuntimeConfiguredOperatorStateLoadException("Configured event store could not be read.", error)
        } catch (error: WorkEventStoreException) {
            throw RuntimeConfiguredOperatorStateLoadException(
                PublicSafeWorkEventStoreMessage.from(
                    error = error,
                    unreadableMessage = "Configured event store could not be read.",
                ),
                error,
            )
        }
    }
}
