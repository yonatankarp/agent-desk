package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.core.domain.entities.WorkItem

data class WorkItemInspection(
    val item: WorkItem,
    val statusPresentation: StatusPresentation,
    val requiresAttention: Boolean,
    val isTerminal: Boolean,
    val acceptedEvents: List<EventLine>,
    val projectionWarnings: List<ProjectionWarningLine>,
)

data class ProjectionWarningLine(
    val eventId: String,
    val reason: String,
)
