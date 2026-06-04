package com.yonatankarp.agentdesk.app.serialization

import kotlinx.serialization.Serializable

@Serializable
data class EvidenceReferenceRecord(
    val kind: String,
    val label: String,
    val target: String,
)
