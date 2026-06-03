package com.yonatankarp.agentdesk.app.config

object AgentDeskRuntimeConfigParser {
    fun parse(values: Map<String, String>): AgentDeskRuntimeConfig {
        val mode = values["mode"]?.let(AgentDeskMode::parse) ?: AgentDeskMode.Sample
        val source = values["source"]?.let(RuntimeEventSourceKind::parse) ?: RuntimeEventSourceKind.Mock
        val eventStoreLocation = values["eventStoreLocation"]?.let(EventStoreLocation::parse)

        return AgentDeskRuntimeConfig(
            mode = mode,
            source = source,
            eventStoreLocation = eventStoreLocation,
        )
    }
}
