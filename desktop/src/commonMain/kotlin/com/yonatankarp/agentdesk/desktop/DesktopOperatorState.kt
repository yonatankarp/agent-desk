package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent

data class DesktopOperatorState(
    val workItems: List<WorkItem>,
    val events: List<WorkEvent>,
)
