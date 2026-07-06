package com.yonatankarp.agentdesk.app.operator.action

import com.yonatankarp.agentdesk.app.operator.audit.AuditEntry
import com.yonatankarp.agentdesk.core.domain.valueobjects.PublicSafeTextPolicy

sealed interface LiveHostInspectOutcome {
    data object WorkItemNotFound : LiveHostInspectOutcome

    data class NotExecuted(
        val reason: String,
        val auditEntries: List<AuditEntry>,
    ) : LiveHostInspectOutcome {
        init {
            require(reason.isNotBlank()) { "Inspect not-executed reason must not be blank." }
            PublicSafeTextPolicy.requirePublicSafe(reason, fieldName = "Inspect not-executed reason")
        }
    }

    data class Executed(
        val proposal: LiveHostInspectProposal,
        val output: LiveHostInspectOutput,
        val auditEntries: List<AuditEntry>,
    ) : LiveHostInspectOutcome
}
