package com.yonatankarp.agentdesk.core.domain.valueobjects

/**
 * Adapter-neutral lifecycle state for supervised agent work.
 *
 * Status is the durable lifecycle claim. Conditions such as "stale" are derived from
 * timestamps or heartbeats and should not be stored as a separate lifecycle value.
 */
enum class WorkStatus {
    Queued,
    Running,
    Waiting,
    NeedsDecision,
    Blocked,
    Succeeded,
    Failed,
    Canceled,
    ;

    val isTerminal: Boolean
        get() = this in terminalStatuses

    val requiresHumanAttention: Boolean
        get() = this == NeedsDecision || this == Blocked

    val isResumable: Boolean
        get() = this in resumableStatuses

    fun canTransitionTo(next: WorkStatus): Boolean = when {
        this == next -> true
        isTerminal -> false
        this == Queued -> next in setOf(Running, Canceled)
        this == Running -> next in setOf(Waiting, NeedsDecision, Blocked, Succeeded, Failed, Canceled)
        this == Waiting -> next in setOf(Running, NeedsDecision, Blocked, Failed, Canceled)
        this == NeedsDecision -> next in setOf(Running, Waiting, Blocked, Failed, Canceled)
        this == Blocked -> next in setOf(Running, Waiting, NeedsDecision, Failed, Canceled)
        else -> false
    }

    companion object {
        private val terminalStatuses = setOf(Succeeded, Failed, Canceled)
        private val resumableStatuses = setOf(NeedsDecision, Blocked, Waiting)
    }
}
