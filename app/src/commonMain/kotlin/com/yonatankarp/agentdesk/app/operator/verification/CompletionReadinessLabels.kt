package com.yonatankarp.agentdesk.app.operator.verification

/**
 * Human-readable labels for [CompletionReadinessState] on operator surfaces.
 * Presentation only: the enum itself is owned by the verification model.
 */
object CompletionReadinessLabels {
    fun labelFor(state: CompletionReadinessState): String = when (state) {
        CompletionReadinessState.Ready -> "Ready"
        CompletionReadinessState.NotReady -> "Not ready"
        CompletionReadinessState.Blocked -> "Blocked"
        CompletionReadinessState.Unknown -> "Unknown"
    }
}
