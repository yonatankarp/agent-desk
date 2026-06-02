package com.yonatankarp.agentdesk.core

@JvmInline
value class WorkItemId private constructor(val value: String) {
    companion object {
        private val validPattern = Regex("[a-z0-9][a-z0-9._:-]{0,63}")

        fun parse(raw: String): WorkItemId {
            val normalized = raw.trim().lowercase()
            require(validPattern.matches(normalized)) {
                "Work item id must be 1-64 chars using lowercase letters, numbers, '.', '_', ':', or '-'"
            }
            return WorkItemId(normalized)
        }
    }

    override fun toString(): String = value
}
