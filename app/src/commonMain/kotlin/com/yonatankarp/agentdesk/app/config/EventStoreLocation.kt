package com.yonatankarp.agentdesk.app.config

@JvmInline
value class EventStoreLocation private constructor(val value: String) {
    companion object {
        fun parse(raw: String): EventStoreLocation {
            val normalized = raw.trim()
            if (normalized.isBlank()) {
                throw ConfigValidationException("eventStoreLocation must be configured for stored event mode")
            }
            if (normalized.any { it == '\n' || it == '\r' }) {
                throw ConfigValidationException("eventStoreLocation must be a single public-safe value")
            }
            if (blockedFragments.any { fragment -> normalized.lowercase().contains(fragment) }) {
                throw ConfigValidationException("eventStoreLocation must not contain private or secret material")
            }
            return EventStoreLocation(normalized)
        }

        private val blockedFragments =
            listOf(
                "/home/",
                "\\users\\",
                "op://",
                "token",
                "secret",
                "discord",
                "openclaw",
            )
    }

    override fun toString(): String = value
}
