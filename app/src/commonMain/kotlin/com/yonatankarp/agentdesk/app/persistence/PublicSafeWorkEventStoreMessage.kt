package com.yonatankarp.agentdesk.app.persistence

internal object PublicSafeWorkEventStoreMessage {
    fun from(
        error: WorkEventStoreException,
        unreadableMessage: String,
    ): String = when (val reason = error.reason) {
        WorkEventStoreFailure.AppendFailed -> unreadableMessage

        is WorkEventStoreFailure.CorruptRecord -> "Corrupt work event record at line ${reason.lineNumber} in configured event store"

        is WorkEventStoreFailure.DuplicateEventId ->
            reason.lineNumber
                ?.let { lineNumber -> "Configured event store contains a duplicate work event id at line $lineNumber." }
                ?: "Configured event store contains a duplicate work event id."

        WorkEventStoreFailure.Unreadable -> unreadableMessage
    }
}
