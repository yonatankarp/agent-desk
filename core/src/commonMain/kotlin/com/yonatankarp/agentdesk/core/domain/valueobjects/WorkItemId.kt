package com.yonatankarp.agentdesk.core.domain.valueobjects

@JvmInline
value class WorkItemId private constructor(val value: String) {
    companion object {
        /** Shared grammar for short public-safe id slugs; reused by app-level ids. */
        val validPattern = "[a-z0-9][a-z0-9._:-]{0,63}".toRegex()

        fun parse(raw: String): WorkItemId {
            val normalized = raw.trim().lowercase()
            require(validPattern.matches(normalized)) {
                "Work item id must be 1-64 chars using lowercase letters, numbers, '.', '_', ':', or '-'"
            }
            PublicSafeTextPolicy.requirePublicSafe(normalized, fieldName = "Work item id")
            return WorkItemId(normalized)
        }
    }

    override fun toString(): String = value
}
