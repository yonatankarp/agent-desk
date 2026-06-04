package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.app.config.AgentDeskMode
import com.yonatankarp.agentdesk.app.config.AgentDeskRuntimeConfig
import com.yonatankarp.agentdesk.app.persistence.LocalFileWorkEventRepository
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
            throw RuntimeConfiguredOperatorStateLoadException(error.publicSafeMessage(), error)
        }
    }

    internal fun WorkEventStoreException.publicSafeMessage(): String {
        val text = message.orEmpty()
        val duplicateLine = Regex("""Duplicate work event id .+ at line (\d+) in configured event store""")
            .matchEntire(text)
            ?.groupValues
            ?.get(1)
        if (duplicateLine != null) {
            return "Configured event store contains a duplicate work event id at line $duplicateLine."
        }
        if (text.startsWith("Duplicate work event id ")) {
            return "Configured event store contains a duplicate work event id."
        }
        if (text.startsWith("Corrupt work event record at line ")) {
            return text
        }
        return "Configured event store could not be read."
    }
}
