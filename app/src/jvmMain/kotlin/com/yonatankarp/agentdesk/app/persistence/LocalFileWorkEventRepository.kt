package com.yonatankarp.agentdesk.app.persistence

import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.serialization.WorkEventJson
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * JVM-local event store. A repository instance caches event ids for append-time
 * duplicate checks; create a new instance or call readAll() after external file mutations.
 */
class LocalFileWorkEventRepository(
    private val storePath: Path,
) : WorkEventRepository {
    private var seenIds: MutableSet<WorkEventId>? = null

    override fun append(event: WorkEvent) {
        val existingIds = loadSeenIds()
        if (event.id in existingIds) {
            throw WorkEventStoreException("Duplicate work event id ${event.id} in configured event store")
        }

        try {
            storePath.parent?.let(Files::createDirectories)
            Files.writeString(
                storePath,
                WorkEventJson.encode(event) + "\n",
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
            )
            existingIds += event.id
        } catch (error: IOException) {
            throw WorkEventStoreException("Unable to append work event to configured event store", error)
        }
    }

    override fun readAll(): List<WorkEvent> {
        val snapshot = readSnapshot()
        seenIds = snapshot.eventIds
        return snapshot.events
    }

    private fun loadSeenIds(): MutableSet<WorkEventId> {
        val cached = seenIds
        if (cached != null) {
            return cached
        }

        return readSnapshot().eventIds.also { seenIds = it }
    }

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
                    "Duplicate work event id ${event.id} at line ${index + 1} in configured event store",
                )
            }
            event
        }

        return EventStoreSnapshot(events = events, eventIds = seenIds)
    }

    private fun readLines(): List<String> = try {
        Files.readAllLines(storePath)
    } catch (error: IOException) {
        throw WorkEventStoreException("Unable to read configured event store", error)
    }

    private fun decode(
        line: String,
        lineNumber: Int,
    ): WorkEvent = try {
        WorkEventJson.decode(line)
    } catch (error: IllegalArgumentException) {
        throw WorkEventStoreException("Corrupt work event record at line $lineNumber in configured event store", error)
    } catch (error: RuntimeException) {
        throw WorkEventStoreException("Corrupt work event record at line $lineNumber in configured event store", error)
    }

    private data class EventStoreSnapshot(
        val events: List<WorkEvent>,
        val eventIds: MutableSet<WorkEventId>,
    )
}
