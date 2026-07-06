package com.yonatankarp.agentdesk.app.operator.action

import com.yonatankarp.agentdesk.app.runtime.RuntimeHostAlias
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostOperation
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.valueobjects.PublicSafeTextPolicy
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId

data class LiveHostInspectProposal(
    val proposalId: String,
    val correlationId: String,
    val hostAlias: RuntimeHostAlias,
    val target: WorkItemId,
    val summary: String,
    val createdAt: EventTimestamp,
    val expiresAt: EventTimestamp,
    val action: String = "inspect",
    val requiredOperation: RuntimeHostOperation = RuntimeHostOperation.InspectActionProposal,
) {
    init {
        require(proposalId.isNotBlank()) { "Inspect proposal id must not be blank." }
        require(correlationId.isNotBlank()) { "Inspect correlation id must not be blank." }
        require(summary.isNotBlank()) { "Inspect proposal summary must not be blank." }
        require(expiresAt > createdAt) { "Inspect proposal expiration must be after creation." }
        PublicSafeTextPolicy.requirePublicSafe(proposalId, fieldName = "Inspect proposal id")
        PublicSafeTextPolicy.requirePublicSafe(correlationId, fieldName = "Inspect correlation id")
        PublicSafeTextPolicy.requirePublicSafe(summary, fieldName = "Inspect proposal summary")
    }

    companion object {
        fun idFor(
            target: WorkItemId,
            createdAt: EventTimestamp,
        ): String = "inspect:$target:$createdAt"
    }
}
