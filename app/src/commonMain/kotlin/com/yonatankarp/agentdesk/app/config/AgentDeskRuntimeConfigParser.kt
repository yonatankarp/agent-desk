package com.yonatankarp.agentdesk.app.config

object AgentDeskRuntimeConfigParser {
    fun parse(values: Map<String, String>): AgentDeskRuntimeConfig {
        val defaults = AgentDeskRuntimeConfig.defaults()
        val mode = values["mode"]?.let(AgentDeskMode::parse) ?: defaults.mode
        val source = values["source"]?.let(RuntimeEventSourceKind::parse) ?: defaults.source
        val eventStoreLocation = values["eventStoreLocation"]?.let(EventStoreLocation::parse)

        return AgentDeskRuntimeConfig(
            mode = mode,
            source = source,
            eventStoreLocation = eventStoreLocation,
        )
    }
}
