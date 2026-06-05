package com.yonatankarp.agentdesk.app.persistence

class WorkEventStoreException(
    val reason: WorkEventStoreFailure,
    cause: Throwable? = null,
) : RuntimeException(reason.publicSafeMessage(), cause)

sealed interface WorkEventStoreFailure {
    data class DuplicateEventId(
        val eventId: String,
        val lineNumber: Int? = null,
    ) : WorkEventStoreFailure

    data class CorruptRecord(
        val lineNumber: Int,
    ) : WorkEventStoreFailure

    data object Unreadable : WorkEventStoreFailure

    data object AppendFailed : WorkEventStoreFailure
}

private fun WorkEventStoreFailure.publicSafeMessage(): String = when (this) {
    WorkEventStoreFailure.AppendFailed -> "Unable to append work event to configured event store"

    is WorkEventStoreFailure.CorruptRecord -> "Corrupt work event record at line $lineNumber in configured event store"

    is WorkEventStoreFailure.DuplicateEventId -> {
        val line = lineNumber?.let { " at line $it" }.orEmpty()
        "Duplicate work event id $eventId$line in configured event store"
    }

    WorkEventStoreFailure.Unreadable -> "Unable to read configured event store"
}
