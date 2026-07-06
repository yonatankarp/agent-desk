package com.yonatankarp.agentdesk.app.operator.action

import com.yonatankarp.agentdesk.core.domain.valueobjects.PublicSafeTextPolicy

data class LiveHostInspectOutput(
    val summary: String,
    val evidence: List<String>,
) {
    init {
        require(summary.isNotBlank()) { "Inspect output summary must not be blank." }
        PublicSafeTextPolicy.requirePublicSafe(summary, fieldName = "Inspect output summary")
        evidence.forEach { value ->
            PublicSafeTextPolicy.requirePublicSafe(value, fieldName = "Inspect output evidence")
        }
    }
}
