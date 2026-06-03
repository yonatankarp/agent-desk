package com.yonatankarp.agentdesk.app.config

data class AgentDeskRuntimeConfig(
    val mode: AgentDeskMode,
    val source: RuntimeEventSourceKind,
    val eventStoreLocation: EventStoreLocation? = null,
) {
    init {
        when (mode) {
            AgentDeskMode.Sample -> requireSampleMode()
            AgentDeskMode.StoredEvents -> requireStoredEventMode()
        }
    }

    private fun requireSampleMode() {
        if (source != RuntimeEventSourceKind.Mock) {
            throw ConfigValidationException("sample mode requires the mock event source")
        }
        if (eventStoreLocation != null) {
            throw ConfigValidationException("sample mode must not configure an event store location")
        }
    }

    private fun requireStoredEventMode() {
        if (source != RuntimeEventSourceKind.LocalEventStore) {
            throw ConfigValidationException("stored event mode requires the local event store source")
        }
        if (eventStoreLocation == null) {
            throw ConfigValidationException("stored event mode requires eventStoreLocation")
        }
    }

    companion object {
        fun defaults(): AgentDeskRuntimeConfig = AgentDeskRuntimeConfig(
            mode = AgentDeskMode.Sample,
            source = RuntimeEventSourceKind.Mock,
        )
    }
}
