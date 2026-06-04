package com.yonatankarp.agentdesk.app.operator.mobile

data class MobileWorkItem(
    val id: String,
    val title: String,
    val summary: String?,
    val status: MobileStatusPresentation,
    val evidenceReferences: List<MobileEvidenceReference> = emptyList(),
)
