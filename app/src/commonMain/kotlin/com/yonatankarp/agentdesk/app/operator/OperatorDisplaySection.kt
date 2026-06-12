package com.yonatankarp.agentdesk.app.operator

enum class OperatorDisplaySection(
    val canonicalTitle: String,
    val desktopLabel: String,
    val mobileLabel: String,
) {
    ReplayStatus(
        canonicalTitle = "Replay status",
        desktopLabel = "Replay status",
        mobileLabel = "Projection warnings",
    ),
    WorkState(
        canonicalTitle = "Work state",
        desktopLabel = "Work state",
        mobileLabel = "Current work",
    ),
    Timeline(
        canonicalTitle = "Timeline",
        desktopLabel = "Read-only timeline",
        mobileLabel = "Timeline",
    ),
    DecisionQueue(
        canonicalTitle = "Decision queue",
        desktopLabel = "Decision queue",
        mobileLabel = "Attention queue",
    ),
    EvidenceDetail(
        canonicalTitle = "Evidence detail",
        desktopLabel = "Evidence drilldown",
        mobileLabel = "Evidence detail",
    ),
}

object OperatorDisplayStructure {
    val orderedSections: List<OperatorDisplaySection> = OperatorDisplaySection.entries
}
