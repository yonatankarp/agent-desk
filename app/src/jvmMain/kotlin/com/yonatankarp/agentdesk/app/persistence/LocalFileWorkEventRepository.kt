package com.yonatankarp.agentdesk.app.persistence

import com.yonatankarp.agentdesk.app.serialization.WorkEventJson
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import java.nio.file.Path

/**
 * JVM-local event store over the shared [AppendOnlyNdjsonStore] mechanics.
 * Failure wording stays here: the event store interpolates its typed,
 * validated [WorkEventId] into duplicate failures.
 */
class LocalFileWorkEventRepository(
    storePath: Path,
    maxStoreSizeBytes: Long = DEFAULT_MAX_STORE_SIZE_BYTES,
) : WorkEventRepository {
    private val store = AppendOnlyNdjsonStore(
        storePath = storePath,
        maxStoreSizeBytes = maxStoreSizeBytes,
        encode = WorkEventJson::encode,
        decode = WorkEventJson::decode,
        idOf = WorkEvent::id,
        failures = WorkEventStoreFailures,
    )

    override fun append(event: WorkEvent) = store.append(event)

    override fun readAll(): WorkEventReadResult = store.readSnapshot().let { snapshot ->
        WorkEventReadResult(
            events = snapshot.records,
            trailingCorruption = snapshot.tornTrailingLineNumber?.let { lineNumber ->
                TornTrailingRecord(
                    lineNumber = lineNumber,
                    recoveredEventCount = snapshot.records.size,
                )
            },
        )
    }

    private object WorkEventStoreFailures : NdjsonStoreFailures<WorkEventId> {
        override fun appendFailed(cause: Throwable?) = WorkEventStoreException(WorkEventStoreFailure.AppendFailed, cause)

        override fun unreadable(cause: Throwable) = WorkEventStoreException(WorkEventStoreFailure.Unreadable, cause)

        override fun storeTooLarge() = WorkEventStoreException(WorkEventStoreFailure.StoreTooLarge)

        override fun corruptRecord(
            lineNumber: Int,
            cause: Throwable,
        ) = WorkEventStoreException(WorkEventStoreFailure.CorruptRecord(lineNumber), cause)

        override fun duplicateId(
            id: WorkEventId,
            lineNumber: Int?,
        ) = WorkEventStoreException(
            WorkEventStoreFailure.DuplicateEventId(eventId = id.toString(), lineNumber = lineNumber),
        )

        override fun appendBlockedByTornRecord(
            lineNumber: Int,
            recoveredRecordCount: Int,
        ) = WorkEventStoreException(
            WorkEventStoreFailure.AppendBlockedByTornRecord(
                TornTrailingRecord(
                    lineNumber = lineNumber,
                    recoveredEventCount = recoveredRecordCount,
                ),
            ),
        )
    }

    companion object {
        const val DEFAULT_MAX_STORE_SIZE_BYTES: Long = AppendOnlyNdjsonStore.DEFAULT_MAX_STORE_SIZE_BYTES
    }
}
