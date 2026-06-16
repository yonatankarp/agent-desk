package com.yonatankarp.agentdesk.app.operator.timeline

data class ReadOnlyTimelineFilter(
    val projectId: String? = null,
    val workspaceId: String? = null,
    val upstreamSourceId: String? = null,
    val ownerId: String? = null,
    val status: String? = null,
    val risk: String? = null,
    val timeWindow: String? = null,
) {
    fun matches(entry: ReadOnlyTimelineEntry): Boolean = projectId.matches(entry.provenance.projectId) &&
        workspaceId.matches(entry.provenance.workspaceId) &&
        upstreamSourceId.matches(entry.provenance.sourceId) &&
        ownerId.matches(entry.provenance.ownerId) &&
        status.matches(entry.status) &&
        risk.matches(entry.state.name) &&
        timeWindow.matches(entry.timeWindow)

    private fun String?.matches(actual: String?): Boolean = this == null || this == actual
}
