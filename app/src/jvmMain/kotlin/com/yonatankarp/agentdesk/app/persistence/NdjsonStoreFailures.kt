package com.yonatankarp.agentdesk.app.persistence

/**
 * Per-store failure factory for [AppendOnlyNdjsonStore]. Each store maps the
 * shared mechanical failure conditions to its own typed exception and owns the
 * message wording, including whether the record id is safe to surface: the
 * work-event store interpolates its id, while the audit store stays
 * line-number-only as a deliberate conservative public-safety choice.
 */
internal interface NdjsonStoreFailures<ID : Any> {
    fun appendFailed(cause: Throwable?): RuntimeException

    fun unreadable(cause: Throwable): RuntimeException

    fun storeTooLarge(): RuntimeException

    fun corruptRecord(
        lineNumber: Int,
        cause: Throwable,
    ): RuntimeException

    fun duplicateId(
        id: ID,
        lineNumber: Int?,
    ): RuntimeException

    fun appendBlockedByTornRecord(
        lineNumber: Int,
        recoveredRecordCount: Int,
    ): RuntimeException
}
