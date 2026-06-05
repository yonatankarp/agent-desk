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
                        val existingIds = readSnapshot().eventIds
                        if (event.id in existingIds) {
                            throw WorkEventStoreException(
                                WorkEventStoreFailure.DuplicateEventId(eventId = event.id.toString()),
                            )
                        }

                        channel.position(channel.size())
                        val record = ByteBuffer.wrap(
                            (WorkEventJson.encode(event) + "\n").toByteArray(StandardCharsets.UTF_8),
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

    override fun readAll(): List<WorkEvent> = readSnapshot().events

    private fun readSnapshot(): EventStoreSnapshot {
        if (!Files.exists(storePath)) {
            return EventStoreSnapshot(events = emptyList(), eventIds = mutableSetOf())
        }

        val seenIds = mutableSetOf<WorkEventId>()
        val events = readLines().mapIndexedNotNull { index, line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                return@mapIndexedNotNull null
            }

            val event = decode(trimmed, lineNumber = index + 1)
            if (!seenIds.add(event.id)) {
                throw WorkEventStoreException(
                    WorkEventStoreFailure.DuplicateEventId(
                        eventId = event.id.toString(),
                        lineNumber = index + 1,
                    ),
                )
            }
            event
        }

        return EventStoreSnapshot(events = events, eventIds = seenIds)
    }

    private fun readLines(): List<String> = try {
        Files.readAllLines(storePath)
    } catch (error: IOException) {
        throw WorkEventStoreException(WorkEventStoreFailure.Unreadable, error)
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

    private data class EventStoreSnapshot(
        val events: List<WorkEvent>,
        val eventIds: MutableSet<WorkEventId>,
    )

    companion object {
        private val pathLocks = ConcurrentHashMap<Path, ReentrantLock>()

        private fun pathLockFor(storePath: Path): ReentrantLock = pathLocks.computeIfAbsent(storePath.toAbsolutePath().normalize()) {
            ReentrantLock()
        }
    }
}
