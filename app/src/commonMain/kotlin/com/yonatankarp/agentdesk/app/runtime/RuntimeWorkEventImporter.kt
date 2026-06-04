package com.yonatankarp.agentdesk.app.runtime

import com.yonatankarp.agentdesk.app.persistence.PublicSafeWorkEventStoreMessage
import com.yonatankarp.agentdesk.app.persistence.WorkEventRepository
import com.yonatankarp.agentdesk.app.persistence.WorkEventStoreException
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId

class RuntimeWorkEventImporter(
    private val source: RuntimeWorkEventSource,
    private val repository: WorkEventRepository,
) {
    fun importEvents(): RuntimeWorkEventImportResult {
        val existingIds = readExistingIds()
        val events = loadSourceEvents()

        var imported = 0
        var skippedDuplicates = 0
        val acceptedIds = existingIds.toMutableSet()
        events.forEach { event ->
            if (!acceptedIds.add(event.id)) {
                skippedDuplicates += 1
            } else {
                append(event)
                imported += 1
            }
        }

        return RuntimeWorkEventImportResult(
            importedCount = imported,
            skippedDuplicateCount = skippedDuplicates,
        )
    }

    private fun readExistingIds(): Set<WorkEventId> = try {
        repository.readAll().mapTo(mutableSetOf()) { it.id }
    } catch (error: WorkEventStoreException) {
        throw RuntimeWorkEventImportException(error.publicSafeImportMessage(), error)
    }

    private fun loadSourceEvents(): List<WorkEvent> = try {
        source.loadEvents()
    } catch (error: IllegalArgumentException) {
        throw RuntimeWorkEventImportException("Runtime observations could not be imported.", error)
    } catch (error: RuntimeException) {
        throw RuntimeWorkEventImportException("Runtime observations could not be imported.", error)
    }

    private fun append(event: WorkEvent) {
        try {
            repository.append(event)
        } catch (error: WorkEventStoreException) {
            throw RuntimeWorkEventImportException(error.publicSafeImportMessage(), error)
        }
    }

    private fun WorkEventStoreException.publicSafeImportMessage(): String = PublicSafeWorkEventStoreMessage.from(
        error = this,
        unreadableMessage = "Runtime events could not be imported into configured event store.",
    )
}
