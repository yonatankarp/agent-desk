package com.yonatankarp.agentdesk.app.runtime

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class OpenClawRuntimeObservationFileSource(
    private val exportPath: Path,
) : RuntimeWorkEventSource {
    override fun loadObservations(): List<RuntimeWorkObservation> {
        val rawExport = readExport()
        return decodeExport(rawExport)
    }

    private fun readExport(): String = try {
        Files.readString(exportPath)
    } catch (error: IOException) {
        throw OpenClawRuntimeObservationFileSourceException(
            message = "Sanitized observation export could not be read.",
            cause = error,
        )
    } catch (error: SecurityException) {
        throw OpenClawRuntimeObservationFileSourceException(
            message = "Sanitized observation export could not be read.",
            cause = error,
        )
    }

    private fun decodeExport(rawExport: String): List<RuntimeWorkObservation> = try {
        val export = json.decodeFromString<OpenClawObservationExportRecord>(rawExport)
        require(export.schemaVersion == SUPPORTED_SCHEMA_VERSION) {
            "Sanitized observation export schema version is not supported."
        }
        export.observations.mapIndexed { index, record ->
            record.toObservation(index)
        }
    } catch (error: IllegalArgumentException) {
        throw OpenClawRuntimeObservationFileSourceException(
            message = "Sanitized observation export is invalid.",
            cause = error,
        )
    } catch (error: SerializationException) {
        throw OpenClawRuntimeObservationFileSourceException(
            message = "Sanitized observation export is invalid.",
            cause = error,
        )
    }

    private fun OpenClawObservationRecord.toObservation(index: Int): RuntimeWorkObservation {
        require(eventId != null) { "Observation ${index + 1} is missing eventId." }
        require(occurredAt != null) { "Observation ${index + 1} is missing occurredAt." }
        require(source != null) { "Observation ${index + 1} is missing source." }
        require(workItemId != null) { "Observation ${index + 1} is missing workItemId." }
        require(kind != null) { "Observation ${index + 1} is missing kind." }

        return RuntimeWorkObservation(
            eventId = eventId,
            occurredAt = occurredAt,
            source = source,
            workItemId = workItemId,
            kind = kind.toObservationKind(),
            title = title,
            summary = summary,
            reason = reason,
            evidenceReferences = evidenceReferences.map { it.toRuntimeEvidenceReference() },
        )
    }

    private fun String.toObservationKind(): RuntimeWorkObservationKind = when (trim().lowercase()) {
        "started" -> RuntimeWorkObservationKind.Started
        "needs-decision" -> RuntimeWorkObservationKind.NeedsDecision
        "blocked" -> RuntimeWorkObservationKind.Blocked
        "succeeded" -> RuntimeWorkObservationKind.Succeeded
        "failed" -> RuntimeWorkObservationKind.Failed
        "canceled" -> RuntimeWorkObservationKind.Canceled
        else -> throw IllegalArgumentException("Observation kind is not supported.")
    }

    private fun OpenClawEvidenceReferenceRecord.toRuntimeEvidenceReference(): RuntimeEvidenceReference {
        require(kind != null) { "Evidence reference is missing kind." }
        require(label != null) { "Evidence reference is missing label." }
        require(target != null) { "Evidence reference is missing target." }

        return RuntimeEvidenceReference(
            kind = kind,
            label = label,
            target = target,
        )
    }

    private companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1

        val json =
            Json {
                explicitNulls = false
                ignoreUnknownKeys = false
            }
    }
}

class OpenClawRuntimeObservationFileSourceException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

@Serializable
private data class OpenClawObservationExportRecord(
    val schemaVersion: Int,
    val observations: List<OpenClawObservationRecord>,
)

@Serializable
private data class OpenClawObservationRecord(
    val eventId: String? = null,
    val occurredAt: String? = null,
    val source: String? = null,
    val workItemId: String? = null,
    val kind: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val reason: String? = null,
    @SerialName("evidenceReferences")
    val evidenceReferences: List<OpenClawEvidenceReferenceRecord> = emptyList(),
)

@Serializable
private data class OpenClawEvidenceReferenceRecord(
    val kind: String? = null,
    val label: String? = null,
    val target: String? = null,
)
