package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.core.WorkEvent
import com.yonatankarp.agentdesk.core.WorkItem

data class OperatorState(
    val workItems: List<WorkItem>,
    val events: List<WorkEvent>,
)
