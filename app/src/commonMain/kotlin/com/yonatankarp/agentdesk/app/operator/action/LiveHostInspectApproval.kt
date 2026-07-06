package com.yonatankarp.agentdesk.app.operator.action

import com.yonatankarp.agentdesk.app.runtime.RuntimeHostAlias
import com.yonatankarp.agentdesk.core.domain.valueobjects.PublicSafeTextPolicy
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId

sealed interface LiveHostInspectApproval {
    data class Approve(
        val proposalId: String,
        val hostAlias: RuntimeHostAlias,
        val target: WorkItemId,
        val action: String = "inspect",
    ) : LiveHostInspectApproval {
        init {
            require(proposalId.isNotBlank()) { "Inspect approval proposal id must not be blank." }
            require(action.isNotBlank()) { "Inspect approval action must not be blank." }
            PublicSafeTextPolicy.requirePublicSafe(proposalId, fieldName = "Inspect approval proposal id")
            PublicSafeTextPolicy.requirePublicSafe(action, fieldName = "Inspect approval action")
        }
    }

    data object Deny : LiveHostInspectApproval
}
