package com.yonatankarp.agentdesk.app.persistence

internal object PublicSafeWorkEventStoreMessage {
    fun from(
        error: WorkEventStoreException,
        unreadableMessage: String,
    ): String = when (val reason = error.reason) {
        WorkEventStoreFailure.AppendFailed -> unreadableMessage

        is WorkEventStoreFailure.AppendBlockedByTornRecord -> reason.trailingCorruption.publicSafeMessage()

        is WorkEventStoreFailure.CorruptRecord -> "Corrupt work event record at line ${reason.lineNumber} in configured event store"

        is WorkEventStoreFailure.DuplicateEventId ->
            reason.lineNumber
                ?.let { lineNumber -> "Configured event store contains a duplicate work event id at line $lineNumber." }
                ?: "Configured event store contains a duplicate work event id."

        WorkEventStoreFailure.StoreTooLarge -> "Configured event store exceeds the maximum readable size."

        WorkEventStoreFailure.Unreadable -> unreadableMessage
    }
}
