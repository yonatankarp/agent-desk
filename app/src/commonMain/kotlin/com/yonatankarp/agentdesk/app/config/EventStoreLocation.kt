package com.yonatankarp.agentdesk.app.config

import com.yonatankarp.agentdesk.core.domain.valueobjects.PublicSafeTextPolicy

@JvmInline
value class EventStoreLocation private constructor(val value: String) {
    companion object {
        fun parse(raw: String): EventStoreLocation {
            if (raw.trim().isBlank()) {
                throw ConfigValidationException("eventStoreLocation must be configured for stored event mode")
            }
            if (raw.any { it == '\n' || it == '\r' }) {
                throw ConfigValidationException("eventStoreLocation must be a single public-safe value")
            }
            val normalized = raw.requirePublicSafeEventStoreLocation()
            return EventStoreLocation(normalized)
        }

        private fun String.requirePublicSafeEventStoreLocation(): String = try {
            // Stored-event mode accepts local filesystem paths, but shares runtime/id/secret checks.
            PublicSafeTextPolicy.normalizeAndRequirePublicSafeLocalConfigPath(
                raw = this,
                fieldName = "eventStoreLocation",
                maxLength = 512,
            )
        } catch (error: IllegalArgumentException) {
            throw ConfigValidationException(
                error.message ?: "eventStoreLocation must be a public-safe value",
            )
        }
    }

    override fun toString(): String = value
}
