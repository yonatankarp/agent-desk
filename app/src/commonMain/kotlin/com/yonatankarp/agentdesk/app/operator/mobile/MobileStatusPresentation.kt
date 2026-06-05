package com.yonatankarp.agentdesk.app.operator.mobile

import com.yonatankarp.agentdesk.app.operator.StatusTone

data class MobileStatusPresentation(
    val label: String,
    val tone: StatusTone,
)
