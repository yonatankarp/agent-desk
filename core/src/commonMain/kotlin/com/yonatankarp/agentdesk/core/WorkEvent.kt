package com.yonatankarp.agentdesk.core

data class WorkEvent(
    val id: WorkEventId,
    val occurredAt: EventTimestamp,
    val source: EventSource,
    val workItemId: WorkItemId,
    val payload: WorkEventPayload,
) {
    val type: WorkEventType
        get() = payload.type
}

@JvmInline
value class WorkEventId private constructor(val value: String) {
    companion object {
        private val validPattern = Regex("[a-z0-9][a-z0-9._:-]{0,95}")

        fun parse(raw: String): WorkEventId {
            val normalized = raw.trim().lowercase()
            require(validPattern.matches(normalized)) {
                "Work event id must be 1-96 chars using lowercase letters, numbers, '.', '_', ':', or '-'"
            }
            return WorkEventId(normalized)
        }
    }

    override fun toString(): String = value
}

@JvmInline
value class EventTimestamp private constructor(val value: String) {
    companion object {
        private val rfc3339UtcPattern =
            Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,9})?Z")

        fun parse(raw: String): EventTimestamp {
            val normalized = raw.trim()
            require(rfc3339UtcPattern.matches(normalized)) {
                "Event timestamp must be an RFC 3339 UTC instant, for example 2026-06-02T21:00:00Z"
            }
            return EventTimestamp(normalized)
        }
    }

    override fun toString(): String = value
}

@JvmInline
value class EventSource private constructor(val value: String) {
    companion object {
        private val validPattern = Regex("[a-z][a-z0-9]*(?:[._:-][a-z0-9]+){0,7}")

        fun parse(raw: String): EventSource {
            val normalized = raw.trim().lowercase()
            require(validPattern.matches(normalized)) {
                "Event source must be a lowercase adapter-neutral identifier"
            }
            return EventSource(normalized)
        }
    }

    override fun toString(): String = value
}

enum class WorkEventType(val wireName: String) {
    WorkStarted("work.started"),
    WorkBlocked("work.blocked"),
}

sealed interface WorkEventPayload {
    val type: WorkEventType
}

data class WorkStartedPayload(
    val title: WorkItemTitle,
    val summary: WorkSummary? = null,
) : WorkEventPayload {
    override val type: WorkEventType = WorkEventType.WorkStarted
}

data class WorkBlockedPayload(
    val reason: WorkSummary,
) : WorkEventPayload {
    override val type: WorkEventType = WorkEventType.WorkBlocked
}
