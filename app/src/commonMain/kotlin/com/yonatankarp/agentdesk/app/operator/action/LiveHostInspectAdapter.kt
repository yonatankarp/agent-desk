package com.yonatankarp.agentdesk.app.operator.action

import com.yonatankarp.agentdesk.app.runtime.RuntimeHostAlias
import com.yonatankarp.agentdesk.core.domain.valueobjects.PublicSafeTextPolicy
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId

data class LiveHostInspectAdapterRequest(
    val proposalId: String,
    val correlationId: String,
    val hostAlias: RuntimeHostAlias,
    val target: WorkItemId,
) {
    init {
        PublicSafeTextPolicy.requirePublicSafe(proposalId, fieldName = "Inspect adapter proposal id")
        PublicSafeTextPolicy.requirePublicSafe(correlationId, fieldName = "Inspect adapter correlation id")
    }
}

fun interface LiveHostInspectAdapter {
    fun inspect(request: LiveHostInspectAdapterRequest): LiveHostInspectAdapterResult
}

object SyntheticLiveHostInspectAdapter : LiveHostInspectAdapter {
    override fun inspect(request: LiveHostInspectAdapterRequest): LiveHostInspectAdapterResult = LiveHostInspectAdapterResult.Succeeded(
        LiveHostInspectOutput(
            summary = "Synthetic inspect completed for ${request.target} on ${request.hostAlias.value}.",
            evidence = listOf("sanitized-note:synthetic-live-inspect"),
        ),
    )
}
