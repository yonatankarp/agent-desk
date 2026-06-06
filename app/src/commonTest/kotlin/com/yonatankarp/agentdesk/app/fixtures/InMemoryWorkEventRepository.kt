package com.yonatankarp.agentdesk.app.fixtures

import com.yonatankarp.agentdesk.app.persistence.WorkEventReadResult
import com.yonatankarp.agentdesk.app.persistence.WorkEventRepository
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent

/** Plain in-memory repository test double for tests without failure-mode needs. */
internal class InMemoryWorkEventRepository : WorkEventRepository {
    private val events = mutableListOf<WorkEvent>()

    override fun append(event: WorkEvent) {
        events += event
    }

    override fun readAll(): WorkEventReadResult = WorkEventReadResult(events = events.toList())
}
