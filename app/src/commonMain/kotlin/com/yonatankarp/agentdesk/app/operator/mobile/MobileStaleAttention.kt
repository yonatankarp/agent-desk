package com.yonatankarp.agentdesk.app.operator.mobile

data class MobileStaleAttention(
    val lastEventAt: String,
    val staleForMinutes: Long,
)
