package com.yonatankarp.agentdesk.app.runtime

import com.yonatankarp.agentdesk.core.domain.events.WorkEvent

interface RuntimeWorkEventSource {
    fun loadEvents(): List<WorkEvent>
}
