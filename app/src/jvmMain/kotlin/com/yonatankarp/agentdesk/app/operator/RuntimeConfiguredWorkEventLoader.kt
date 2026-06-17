package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.app.config.AgentDeskMode
import com.yonatankarp.agentdesk.app.config.AgentDeskRuntimeConfig
import com.yonatankarp.agentdesk.app.persistence.LocalFileWorkEventRepository
import com.yonatankarp.agentdesk.app.persistence.PublicSafeWorkEventStoreMessage
import com.yonatankarp.agentdesk.app.persistence.WorkEventReadResult
import com.yonatankarp.agentdesk.app.persistence.WorkEventStoreException
import java.nio.file.InvalidPathException
import java.nio.file.Path

object RuntimeConfiguredWorkEventLoader {
    fun load(config: AgentDeskRuntimeConfig): WorkEventReadResult = when (config.mode) {
        AgentDeskMode.Sample -> WorkEventReadResult(events = SampleOperatorState.current().events)
        AgentDeskMode.StoredEvents -> loadStoredEvents(config)
    }

    private fun loadStoredEvents(config: AgentDeskRuntimeConfig): WorkEventReadResult {
        val location = config.eventStoreLocation
            ?: throw RuntimeConfiguredOperatorStateLoadException("Configured event store could not be read.")

        return try {
            LocalFileWorkEventRepository(Path.of(location.value)).readAll()
        } catch (error: InvalidPathException) {
            throw RuntimeConfiguredOperatorStateLoadException("Configured event store could not be read.", error)
        } catch (error: SecurityException) {
            throw RuntimeConfiguredOperatorStateLoadException("Configured event store source permission is missing.", error)
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
