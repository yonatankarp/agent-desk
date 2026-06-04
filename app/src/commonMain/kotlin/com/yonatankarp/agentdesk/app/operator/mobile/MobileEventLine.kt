package com.yonatankarp.agentdesk.app.operator.mobile

data class MobileEventLine(
    val occurredAt: String,
    val type: String,
    val workItemId: String,
    val detail: String,
    val evidenceReferences: List<MobileEvidenceReference> = emptyList(),
)
