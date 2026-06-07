package com.yonatankarp.agentdesk.app.persistence

class AuditStoreException(
    val reason: AuditStoreFailure,
    cause: Throwable? = null,
) : RuntimeException(reason.publicSafeMessage(), cause)

sealed interface AuditStoreFailure {
    /**
     * Duplicate audit record id. The id itself is a free-form string and is
     * deliberately never interpolated into messages; only the line number is.
     */
    data class DuplicateRecordId(
        val lineNumber: Int? = null,
    ) : AuditStoreFailure

    data class CorruptRecord(
        val lineNumber: Int,
    ) : AuditStoreFailure

    data object Unreadable : AuditStoreFailure

    data object StoreTooLarge : AuditStoreFailure

    data object AppendFailed : AuditStoreFailure

    data class AppendBlockedByTornRecord(
        val trailingCorruption: TornTrailingAuditRecord,
    ) : AuditStoreFailure
}

private fun AuditStoreFailure.publicSafeMessage(): String = when (this) {
    AuditStoreFailure.AppendFailed -> "Unable to append audit record to configured audit store"

    is AuditStoreFailure.AppendBlockedByTornRecord -> trailingCorruption.publicSafeMessage()

    is AuditStoreFailure.CorruptRecord -> "Corrupt audit record at line $lineNumber in configured audit store"

    is AuditStoreFailure.DuplicateRecordId -> {
        val line = lineNumber?.let { " at line $it" }.orEmpty()
        "Duplicate audit record id$line in configured audit store"
    }

    AuditStoreFailure.StoreTooLarge -> "Configured audit store exceeds the maximum readable size"

    AuditStoreFailure.Unreadable -> "Unable to read configured audit store"
}
