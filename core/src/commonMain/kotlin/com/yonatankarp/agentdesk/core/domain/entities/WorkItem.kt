package com.yonatankarp.agentdesk.core.domain.entities

import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary

data class WorkItem(
    val id: WorkItemId,
    val title: WorkItemTitle,
    val status: WorkStatus,
    val summary: WorkSummary? = null,
) {
    fun transitionTo(next: WorkStatus): WorkItem {
        require(status.canTransitionTo(next)) {
            "Cannot transition work item $id from $status to $next"
        }
        return copy(status = next)
    }
}
