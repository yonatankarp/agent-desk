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
    Settings(
        canonicalTitle = "Settings",
        desktopLabel = "Settings",
        mobileLabel = "Settings",
    ),
}

object OperatorDisplayStructure {
    val orderedSections: List<OperatorDisplaySection> = listOf(
        OperatorDisplaySection.ReplayStatus,
        OperatorDisplaySection.WorkState,
        OperatorDisplaySection.Timeline,
        OperatorDisplaySection.DecisionQueue,
        OperatorDisplaySection.EvidenceDetail,
    )
    val settingsSection: OperatorDisplaySection = OperatorDisplaySection.Settings
}
