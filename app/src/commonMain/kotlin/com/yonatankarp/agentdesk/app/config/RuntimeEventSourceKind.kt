package com.yonatankarp.agentdesk.app.config

enum class RuntimeEventSourceKind(val wireName: String) {
    Mock("mock"),
    LocalEventStore("local-event-store"),
    ;

    companion object {
        fun parse(raw: String): RuntimeEventSourceKind = entries.firstOrNull { it.wireName == raw.trim() }
            ?: throw ConfigValidationException("source must be mock or local-event-store")
    }
}
