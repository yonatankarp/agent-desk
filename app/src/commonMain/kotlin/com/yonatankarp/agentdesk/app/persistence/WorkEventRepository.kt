package com.yonatankarp.agentdesk.app.persistence

import com.yonatankarp.agentdesk.core.domain.events.WorkEvent

interface WorkEventRepository {
    fun append(event: WorkEvent)

    fun readAll(): List<WorkEvent>
}
