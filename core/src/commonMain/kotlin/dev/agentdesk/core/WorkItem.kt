package dev.agentdesk.core

data class WorkItem(
    val id: WorkItemId,
    val title: WorkItemTitle,
    val status: WorkStatus,
    val summary: WorkSummary? = null,
) {
    fun transitionTo(next: WorkStatus): WorkItem {
        require(status.canTransitionTo(next)) {
            "Cannot transition work item $id from $status to $next"
        }
        return copy(status = next)
    }
}

@JvmInline
value class WorkItemTitle private constructor(val value: String) {
    companion object {
        private const val MaxLength = 120

        fun parse(raw: String): WorkItemTitle {
            val normalized = raw.trim().replace(Regex("\\s+"), " ")
            require(normalized.isNotEmpty()) {
                "Work item title must not be blank"
            }
            require(raw.lines().size == 1) {
                "Work item title must be a single line"
            }
            require(normalized.length <= MaxLength) {
                "Work item title must be $MaxLength characters or fewer"
            }
            return WorkItemTitle(normalized)
        }
    }

    override fun toString(): String = value
}

@JvmInline
value class WorkSummary private constructor(val value: String) {
    companion object {
        private const val MaxLength = 280

        fun parse(raw: String): WorkSummary {
            val normalized = raw.trim().replace(Regex("\\s+"), " ")
            require(normalized.isNotEmpty()) {
                "Work summary must not be blank"
            }
            require(raw.lines().size == 1) {
                "Work summary must be a single line"
            }
            require(normalized.length <= MaxLength) {
                "Work summary must be $MaxLength characters or fewer"
            }
            return WorkSummary(normalized)
        }
    }

    override fun toString(): String = value
}
