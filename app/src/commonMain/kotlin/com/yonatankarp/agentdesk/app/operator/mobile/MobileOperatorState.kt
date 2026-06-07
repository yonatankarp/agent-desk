package com.yonatankarp.agentdesk.app.operator.mobile

data class MobileOperatorState(
    val currentWork: List<MobileWorkItem>,
    val attentionQueue: List<MobileAttentionItem>,
    val recentEvents: List<MobileEventLine>,
    val projectionWarnings: List<MobileProjectionWarning> = emptyList(),
    val timeline: List<MobileTimelineEntry> = emptyList(),
    val timelineStatusMarkers: List<String> = emptyList(),
    val evidenceDetails: List<MobileEvidenceDetail> = emptyList(),
)
