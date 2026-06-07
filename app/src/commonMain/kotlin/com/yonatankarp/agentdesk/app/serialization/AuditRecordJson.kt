package com.yonatankarp.agentdesk.app.serialization

import com.yonatankarp.agentdesk.app.operator.Actor
import com.yonatankarp.agentdesk.app.operator.audit.AuditActorKind
import com.yonatankarp.agentdesk.app.operator.audit.AuditEntry
import com.yonatankarp.agentdesk.app.operator.audit.AuditEntryId
import com.yonatankarp.agentdesk.app.operator.audit.AuditResult
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Wire codec for persisted audit records. Decoding reconstructs every field
 * through its value-object `parse()` and the `AuditEntry` init validation, so a
 * hand-edited store line cannot smuggle non-public-safe content past load.
 */
object AuditRecordJson {
    private val json =
        Json {
            encodeDefaults = false
            explicitNulls = false
            ignoreUnknownKeys = false
        }

    fun encode(entry: AuditEntry): String = json.encodeToString(toRecord(entry))

    fun decode(raw: String): AuditEntry = json.decodeFromString<AuditRecordRecord>(raw).toDomain()

    private fun toRecord(entry: AuditEntry): AuditRecordRecord = AuditRecordRecord(
        id = entry.id.toString(),
        actor = entry.actor.toString(),
        actorKind = entry.actorKind.wireName(),
        timestamp = entry.timestamp.toString(),
        recordedAt = entry.recordedAt.toString(),
        action = entry.action,
        target = entry.target.toString(),
        result = entry.result.wireName(),
        sourceItem = entry.sourceItem.toString(),
        correlationId = entry.correlationId,
        evidenceReference = entry.evidenceReference.toRecord(),
        detail = entry.detail,
    )

    private fun AuditRecordRecord.toDomain(): AuditEntry = AuditEntry(
        id = AuditEntryId.parse(id),
        actor = Actor.parse(actor),
        actorKind = actorKind.toActorKind(),
        timestamp = EventTimestamp.parse(timestamp),
        recordedAt = EventTimestamp.parse(recordedAt),
        action = action,
        target = WorkItemId.parse(target),
        result = result.toAuditResult(),
        sourceItem = WorkItemId.parse(sourceItem),
        correlationId = correlationId,
        evidenceReference = evidenceReference.toDomain(),
        detail = detail,
    )

    private fun AuditActorKind.wireName(): String = name.lowercase()

    private fun String.toActorKind(): AuditActorKind = AuditActorKind.entries.firstOrNull { it.wireName() == this }
        ?: throw IllegalArgumentException("Unknown audit actor kind")

    private fun AuditResult.wireName(): String = when (this) {
        AuditResult.Approved -> "approved"
        AuditResult.Rejected -> "rejected"
        AuditResult.Deferred -> "deferred"
        AuditResult.RequiresClarification -> "requires-clarification"
        AuditResult.Canceled -> "canceled"
        AuditResult.Failed -> "failed"
        AuditResult.PartialSuccess -> "partial-success"
        AuditResult.Unsupported -> "unsupported"
        AuditResult.Imported -> "imported"
    }

    private fun String.toAuditResult(): AuditResult = AuditResult.entries.firstOrNull { it.wireName() == this }
        ?: throw IllegalArgumentException("Unknown audit result")
}
