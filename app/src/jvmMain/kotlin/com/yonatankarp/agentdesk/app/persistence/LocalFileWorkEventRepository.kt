package com.yonatankarp.agentdesk.app.persistence

import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.serialization.WorkEventJson
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class LocalFileWorkEventRepository(
    private val storePath: Path,
) : WorkEventRepository {
    override fun append(event: WorkEvent) {
        val existingIds = readAll().mapTo(mutableSetOf()) { it.id }
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
        } catch (error: IOException) {
            throw WorkEventStoreException("Unable to append work event to configured event store", error)
        }
    }

    override fun readAll(): List<WorkEvent> {
        if (!Files.exists(storePath)) {
            return emptyList()
        }

        val seenIds = mutableSetOf<WorkEventId>()
        return readLines().mapIndexedNotNull { index, line ->
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
}
