package com.yonatankarp.agentdesk.app.persistence

import com.yonatankarp.agentdesk.app.operator.audit.AuditEntry
import com.yonatankarp.agentdesk.app.operator.audit.AuditEntryId
import com.yonatankarp.agentdesk.app.serialization.AuditRecordJson
import java.nio.file.Path

/**
 * JVM-local audit record store over the shared [AppendOnlyNdjsonStore]
 * mechanics. Failure wording stays here: audit messages are line-number-only
 * as a deliberate conservative public-safety choice — the record id is never
 * interpolated, so the factory discards it.
 */
class LocalFileAuditRecordRepository(
    storePath: Path,
    maxStoreSizeBytes: Long = DEFAULT_MAX_STORE_SIZE_BYTES,
) : AuditRecordRepository {
    private val store = AppendOnlyNdjsonStore(
        storePath = storePath,
        maxStoreSizeBytes = maxStoreSizeBytes,
        encode = AuditRecordJson::encode,
        decode = AuditRecordJson::decode,
        idOf = AuditEntry::id,
        failures = AuditStoreFailures,
    )

    override fun append(entry: AuditEntry) = store.append(entry)

    override fun readAll(): AuditRecordReadResult = store.readSnapshot().let { snapshot ->
        AuditRecordReadResult(
            entries = snapshot.records,
            trailingCorruption = snapshot.tornTrailingLineNumber?.let { lineNumber ->
                TornTrailingAuditRecord(
                    lineNumber = lineNumber,
                    recoveredEntryCount = snapshot.records.size,
                )
            },
        )
    }

    private object AuditStoreFailures : NdjsonStoreFailures<AuditEntryId> {
        override fun appendFailed(cause: Throwable?) = AuditStoreException(AuditStoreFailure.AppendFailed, cause)

        override fun unreadable(cause: Throwable) = AuditStoreException(AuditStoreFailure.Unreadable, cause)

        override fun storeTooLarge() = AuditStoreException(AuditStoreFailure.StoreTooLarge)

        override fun corruptRecord(
            lineNumber: Int,
            cause: Throwable,
        ) = AuditStoreException(AuditStoreFailure.CorruptRecord(lineNumber), cause)

        override fun duplicateId(
            id: AuditEntryId,
            lineNumber: Int?,
        ) = AuditStoreException(AuditStoreFailure.DuplicateRecordId(lineNumber = lineNumber))

        override fun appendBlockedByTornRecord(
            lineNumber: Int,
            recoveredRecordCount: Int,
        ) = AuditStoreException(
            AuditStoreFailure.AppendBlockedByTornRecord(
                TornTrailingAuditRecord(
                    lineNumber = lineNumber,
                    recoveredEntryCount = recoveredRecordCount,
                ),
            ),
        )
    }

    companion object {
        const val DEFAULT_MAX_STORE_SIZE_BYTES: Long = AppendOnlyNdjsonStore.DEFAULT_MAX_STORE_SIZE_BYTES
    }
}
