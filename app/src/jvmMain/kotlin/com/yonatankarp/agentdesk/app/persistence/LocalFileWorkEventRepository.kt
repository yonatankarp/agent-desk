package com.yonatankarp.agentdesk.app.persistence

import com.yonatankarp.agentdesk.app.serialization.WorkEventJson
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.OverlappingFileLockException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * JVM-local event store. Appends serialize through a process-local path lock and
 * a cooperative file lock, then re-read the store before writing.
 */
class LocalFileWorkEventRepository(
    private val storePath: Path,
    private val maxStoreSizeBytes: Long = DEFAULT_MAX_STORE_SIZE_BYTES,
) : WorkEventRepository {
    override fun append(event: WorkEvent) {
        try {
            storePath.parent?.let(Files::createDirectories)
        } catch (error: IOException) {
            throw WorkEventStoreException(WorkEventStoreFailure.AppendFailed, error)
        }

        pathLockFor(storePath).withLock {
            try {
                FileChannel.open(
                    storePath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                ).use { channel ->
                    channel.lock().use {
                        val snapshot = readSnapshot()
                        snapshot.trailingCorruption?.let { corruption ->
                            throw WorkEventStoreException(
                                WorkEventStoreFailure.AppendBlockedByTornRecord(corruption),
                            )
                        }
                        if (event.id in snapshot.eventIds) {
                            throw WorkEventStoreException(
                                WorkEventStoreFailure.DuplicateEventId(eventId = event.id.toString()),
                            )
                        }

                        channel.position(channel.size())
                        val isolatingNewline = if (snapshot.endsWithoutNewline) "\n" else ""
                        val record = ByteBuffer.wrap(
                            (isolatingNewline + WorkEventJson.encode(event) + "\n").toByteArray(StandardCharsets.UTF_8),
                        )
                        while (record.hasRemaining()) {
                            channel.write(record)
                        }
                        channel.force(false)
                    }
                }
            } catch (error: IOException) {
                throw WorkEventStoreException(WorkEventStoreFailure.AppendFailed, error)
            } catch (error: OverlappingFileLockException) {
                throw WorkEventStoreException(WorkEventStoreFailure.AppendFailed, error)
            }
        }
    }

    override fun readAll(): WorkEventReadResult = readSnapshot().let {
        WorkEventReadResult(events = it.events, trailingCorruption = it.trailingCorruption)
    }

    private fun readSnapshot(): EventStoreSnapshot {
        if (!Files.exists(storePath)) {
            return EventStoreSnapshot(events = emptyList(), eventIds = mutableSetOf())
        }

        val text = readText()
        val endsWithoutNewline = text.isNotEmpty() && !text.endsWith("\n")
        val lines = text.split("\n")

        val seenIds = mutableSetOf<WorkEventId>()
        val events = mutableListOf<WorkEvent>()
        var trailingCorruption: TornTrailingRecord? = null
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                return@forEachIndexed
            }

            val lineNumber = index + 1
            val isUnterminatedFinalLine = endsWithoutNewline && index == lines.lastIndex
            val event = if (isUnterminatedFinalLine) {
                // A final line the writer never newline-terminated is a torn
                // write from an interrupted append: recover the committed
                // prefix instead of discarding the whole history.
                decodeOrNull(trimmed) ?: run {
                    trailingCorruption = TornTrailingRecord(
                        lineNumber = lineNumber,
                        recoveredEventCount = events.size,
                    )
                    return@forEachIndexed
                }
            } else {
                // Newline-terminated records were fully written; failing to
                // decode one is real corruption and stays a hard failure.
                decode(trimmed, lineNumber = lineNumber)
            }
            if (!seenIds.add(event.id)) {
                throw WorkEventStoreException(
                    WorkEventStoreFailure.DuplicateEventId(
                        eventId = event.id.toString(),
                        lineNumber = lineNumber,
                    ),
                )
            }
            events += event
        }

        return EventStoreSnapshot(
            events = events,
            eventIds = seenIds,
            trailingCorruption = trailingCorruption,
            endsWithoutNewline = endsWithoutNewline,
        )
    }

    private fun readText(): String {
        val storeSize = try {
            Files.size(storePath)
        } catch (error: IOException) {
            throw WorkEventStoreException(WorkEventStoreFailure.Unreadable, error)
        }
        if (storeSize > maxStoreSizeBytes) {
            throw WorkEventStoreException(WorkEventStoreFailure.StoreTooLarge)
        }

        return try {
            String(Files.readAllBytes(storePath), StandardCharsets.UTF_8)
        } catch (error: IOException) {
            throw WorkEventStoreException(WorkEventStoreFailure.Unreadable, error)
        }
    }

    private fun decode(
        line: String,
        lineNumber: Int,
    ): WorkEvent = try {
        WorkEventJson.decode(line)
    } catch (error: IllegalArgumentException) {
        throw WorkEventStoreException(WorkEventStoreFailure.CorruptRecord(lineNumber), error)
    } catch (error: RuntimeException) {
        throw WorkEventStoreException(WorkEventStoreFailure.CorruptRecord(lineNumber), error)
    }

    private fun decodeOrNull(line: String): WorkEvent? = try {
        WorkEventJson.decode(line)
    } catch (error: RuntimeException) {
        null
    }

    private data class EventStoreSnapshot(
        val events: List<WorkEvent>,
        val eventIds: MutableSet<WorkEventId>,
        val trailingCorruption: TornTrailingRecord? = null,
        val endsWithoutNewline: Boolean = false,
    )

    companion object {
        const val DEFAULT_MAX_STORE_SIZE_BYTES: Long = 10L * 1024 * 1024

        private val pathLocks = ConcurrentHashMap<Path, ReentrantLock>()

        private fun pathLockFor(storePath: Path): ReentrantLock = pathLocks.computeIfAbsent(storePath.toAbsolutePath().normalize()) {
            ReentrantLock()
        }
    }
}
