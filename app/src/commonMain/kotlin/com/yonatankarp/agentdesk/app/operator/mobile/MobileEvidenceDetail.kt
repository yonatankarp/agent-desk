package com.yonatankarp.agentdesk.app.operator.mobile

/**
 * Read-only evidence detail for one timeline entry: the sanitized fields the
 * desktop drilldown exposes (source, timestamp, summary, provenance, related
 * items), derived from already-validated replay state. Raw provider payloads
 * are never part of this model.
 */
data class MobileEvidenceDetail(
    val eventId: String,
    val source: String,
    val timestamp: String,
    val summary: String,
    val provenance: String,
    val evidenceReferences: List<MobileEvidenceReference> = emptyList(),
    val relatedEvents: List<MobileEventLine> = emptyList(),
)
