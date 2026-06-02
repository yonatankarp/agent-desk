package com.yonatankarp.agentdesk.core.domain.valueobjects

@JvmInline
value class WorkItemTitle private constructor(val value: String) {
    companion object {
        private const val MAX_LENGTH = 120

        fun parse(raw: String): WorkItemTitle {
            val normalized = raw.trim().replace(Regex("\\s+"), " ")
            require(normalized.isNotEmpty()) {
                "Work item title must not be blank"
            }
            require(raw.lines().size == 1) {
                "Work item title must be a single line"
            }
            require(normalized.length <= MAX_LENGTH) {
                "Work item title must be $MAX_LENGTH characters or fewer"
            }
            return WorkItemTitle(normalized)
        }
    }

    override fun toString(): String = value
}

@JvmInline
value class WorkSummary private constructor(val value: String) {
    companion object {
        private const val MAX_LENGTH = 280

        fun parse(raw: String): WorkSummary {
            val normalized = raw.trim().replace(Regex("\\s+"), " ")
            require(normalized.isNotEmpty()) {
                "Work summary must not be blank"
            }
            require(raw.lines().size == 1) {
                "Work summary must be a single line"
            }
            require(normalized.length <= MAX_LENGTH) {
                "Work summary must be $MAX_LENGTH characters or fewer"
            }
            return WorkSummary(normalized)
        }
    }

    override fun toString(): String = value
}
