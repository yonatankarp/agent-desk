package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.app.config.AgentDeskMode
import com.yonatankarp.agentdesk.app.config.AgentDeskRuntimeConfig
import com.yonatankarp.agentdesk.app.persistence.LocalFileWorkEventRepository
import com.yonatankarp.agentdesk.app.persistence.WorkEventStoreException
import java.nio.file.InvalidPathException
import java.nio.file.Path

object RuntimeConfiguredOperatorStateLoader {
    fun load(config: AgentDeskRuntimeConfig): OperatorState = when (config.mode) {
        AgentDeskMode.Sample -> SampleOperatorState.current()
        AgentDeskMode.StoredEvents -> loadStoredEvents(config)
    }

    private fun loadStoredEvents(config: AgentDeskRuntimeConfig): OperatorState {
        val location = config.eventStoreLocation
            ?: throw RuntimeConfiguredOperatorStateLoadException("Configured event store could not be read.")

        return try {
            val events = LocalFileWorkEventRepository(Path.of(location.value)).readAll()
            OperatorStateProjector.project(events)
        } catch (error: InvalidPathException) {
            throw RuntimeConfiguredOperatorStateLoadException("Configured event store could not be read.", error)
        } catch (error: SecurityException) {
            throw RuntimeConfiguredOperatorStateLoadException("Configured event store could not be read.", error)
        } catch (error: WorkEventStoreException) {
            throw RuntimeConfiguredOperatorStateLoadException(error.publicSafeMessage(), error)
        } catch (error: OperatorStateProjectionException) {
            throw RuntimeConfiguredOperatorStateLoadException(error.message ?: "Configured event store could not be projected.", error)
        }
    }

    private fun WorkEventStoreException.publicSafeMessage(): String {
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
