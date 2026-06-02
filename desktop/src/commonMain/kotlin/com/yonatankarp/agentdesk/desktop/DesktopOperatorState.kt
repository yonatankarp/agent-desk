package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.core.WorkEvent
import com.yonatankarp.agentdesk.core.WorkItem

data class DesktopOperatorState(
    val workItems: List<WorkItem>,
    val events: List<WorkEvent>,
)
