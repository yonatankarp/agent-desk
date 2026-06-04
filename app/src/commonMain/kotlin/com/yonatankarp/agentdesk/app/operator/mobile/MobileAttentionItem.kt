package com.yonatankarp.agentdesk.app.operator.mobile

data class MobileAttentionItem(
    val workItem: MobileWorkItem,
    val reason: String?,
    val stale: MobileStaleAttention? = null,
)
