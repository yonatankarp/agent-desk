package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent

data class OperatorState(
    val workItems: List<WorkItem>,
    val events: List<WorkEvent>,
)
