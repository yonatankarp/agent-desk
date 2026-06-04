package com.yonatankarp.agentdesk.core.domain.projections

import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus

data class StaleWorkAttention(
    val workItemId: WorkItemId,
    val status: WorkStatus,
    val lastEventAt: EventTimestamp,
    val staleForMinutes: Long,
)

@JvmInline
value class StaleWorkThreshold private constructor(val minutes: Long) {
    companion object {
        val default: StaleWorkThreshold = parseMinutes(60)

        fun parseMinutes(minutes: Long): StaleWorkThreshold {
            require(minutes > 0) { "Stale work threshold must be greater than zero minutes" }
            return StaleWorkThreshold(minutes)
        }
    }
}
