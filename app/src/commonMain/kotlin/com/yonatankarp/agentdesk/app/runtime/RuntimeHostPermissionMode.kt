package com.yonatankarp.agentdesk.app.runtime

enum class RuntimeHostPermissionMode(
    val wireName: String,
    private val allowed: Set<RuntimeHostOperation>,
) {
    DiagnosticOnly(
        wireName = "diagnostic-only",
        allowed = setOf(RuntimeHostOperation.ReachabilityDiagnostic),
    ),
    ReadOnlyObservation(
        wireName = "read-only-observation",
        allowed = setOf(
            RuntimeHostOperation.ReachabilityDiagnostic,
            RuntimeHostOperation.ReadObservation,
        ),
    ),
    ActionCapable(
        wireName = "action-capable",
        allowed = setOf(
            RuntimeHostOperation.ReachabilityDiagnostic,
            RuntimeHostOperation.ReadObservation,
            RuntimeHostOperation.InspectActionProposal,
        ),
    ),
    Unsupported(
        wireName = "unsupported",
        allowed = emptySet(),
    ),
    ;

    fun allows(operation: RuntimeHostOperation): Boolean = operation in allowed

    fun allowedOperationNames(): List<String> = RuntimeHostOperation.entries
        .filter(::allows)
        .map(RuntimeHostOperation::wireName)
}
