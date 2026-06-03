package com.yonatankarp.agentdesk.app.config

enum class AgentDeskMode(val wireName: String) {
    Sample("sample"),
    StoredEvents("stored-events"),
    ;

    companion object {
        fun parse(raw: String): AgentDeskMode = entries.firstOrNull { it.wireName == raw.trim() }
            ?: throw ConfigValidationException("mode must be sample or stored-events")
    }
}
