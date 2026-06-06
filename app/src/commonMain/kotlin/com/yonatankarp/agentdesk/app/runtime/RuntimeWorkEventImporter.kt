package com.yonatankarp.agentdesk.app.runtime

import com.yonatankarp.agentdesk.app.persistence.PublicSafeWorkEventStoreMessage
import com.yonatankarp.agentdesk.app.persistence.WorkEventRepository
import com.yonatankarp.agentdesk.app.persistence.WorkEventStoreException
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId

class RuntimeWorkEventImporter(
    private val source: RuntimeWorkEventSource,
    private val repository: WorkEventRepository,
    private val mapper: SanitizedRuntimeObservationMapper = SanitizedRuntimeObservationMapper(),
) {
    fun importEvents(): RuntimeWorkEventImportResult {
        val existingIds = readExistingIds()
        val events = loadSourceEvents()

        var imported = 0
        var skippedDuplicates = 0
        val diagnostics = mutableListOf<RuntimeWorkEventImportDiagnostic>()
        val acceptedIds = existingIds.toMutableSet()
        events.forEach { event ->
            if (!acceptedIds.add(event.id)) {
                skippedDuplicates += 1
                diagnostics += RuntimeWorkEventImportDiagnostic(
                    kind = RuntimeWorkEventImportDiagnosticKind.SkippedDuplicate,
                    message = "Skipped duplicate runtime event.",
                    eventId = event.id.toString(),
                )
            } else {
                append(event)
                imported += 1
                diagnostics += RuntimeWorkEventImportDiagnostic(
                    kind = RuntimeWorkEventImportDiagnosticKind.Imported,
                    message = "Imported runtime event.",
                    eventId = event.id.toString(),
                )
            }
        }

        return RuntimeWorkEventImportResult(
            importedCount = imported,
            skippedDuplicateCount = skippedDuplicates,
            diagnostics = diagnostics,
        )
    }

    private fun readExistingIds(): Set<WorkEventId> = try {
        repository.readAll().mapTo(mutableSetOf()) { it.id }
    } catch (error: WorkEventStoreException) {
        throw RuntimeWorkEventImportException(
            message = error.publicSafeImportMessage(),
            diagnostics = listOf(
                RuntimeWorkEventImportDiagnostic(
                    kind = RuntimeWorkEventImportDiagnosticKind.StoreRejected,
                    message = "Configured event store could not be read.",
                ),
            ),
            cause = error,
        )
    }

    private fun loadSourceEvents(): List<WorkEvent> = try {
        source.loadObservations().map(mapper::toWorkEvent)
    } catch (error: IllegalArgumentException) {
        throw RuntimeWorkEventImportException(
            message = "Runtime observations could not be imported.",
            diagnostics = listOf(error.toSourceDiagnostic()),
            cause = error,
        )
    } catch (error: RuntimeException) {
        throw RuntimeWorkEventImportException(
            message = "Runtime observations could not be imported.",
            diagnostics = listOf(error.toSourceDiagnostic()),
            cause = error,
        )
    }

    private fun append(event: WorkEvent) {
        try {
            repository.append(event)
        } catch (error: WorkEventStoreException) {
            throw RuntimeWorkEventImportException(
                message = error.publicSafeImportMessage(),
                diagnostics = listOf(
                    RuntimeWorkEventImportDiagnostic(
                        kind = RuntimeWorkEventImportDiagnosticKind.StoreRejected,
                        message = "Configured event store rejected a runtime event.",
                    ),
                ),
                cause = error,
            )
        }
    }

    private fun WorkEventStoreException.publicSafeImportMessage(): String = PublicSafeWorkEventStoreMessage.from(
        error = this,
        unreadableMessage = "Runtime events could not be imported into configured event store.",
    )

    private fun RuntimeException.toSourceDiagnostic(): RuntimeWorkEventImportDiagnostic {
        val normalizedMessage = message.orEmpty()
        val kind = if (normalizedMessage.referencesPublicSafetyBoundary()) {
            RuntimeWorkEventImportDiagnosticKind.UnsafeRejected
        } else {
            RuntimeWorkEventImportDiagnosticKind.InvalidSource
        }

        return RuntimeWorkEventImportDiagnostic(
            kind = kind,
            message = "Runtime observation source was rejected.",
        )
    }

    private fun String.referencesPublicSafetyBoundary(): Boolean {
        val publicSafetyMarkers = listOf(
            "unsafe",
            "public-safe",
            "private",
            "credential",
            "token",
            "path",
            "channel",
            "transcript",
            "runtime",
        )

        return publicSafetyMarkers.any { contains(it, ignoreCase = true) }
    }
}
