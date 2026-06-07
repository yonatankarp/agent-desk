package com.yonatankarp.agentdesk.app.operator.verification

import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReference
import com.yonatankarp.agentdesk.core.domain.valueobjects.PublicSafeTextPolicy

data class VerificationResult(
    val name: String,
    val kind: VerificationKind,
    val result: VerificationState,
    val durationMillis: Long?,
    val outputReference: String,
    val failureSummary: String?,
    val evidenceReference: EvidenceReference,
    val inputBinding: VerificationInputBinding? = null,
) {
    init {
        require(name.isNotBlank()) { "Verification result name must not be blank" }
        require(outputReference.isNotBlank()) { "Verification output reference must not be blank" }
        require(durationMillis == null || durationMillis >= 0) {
            "Verification duration must not be negative."
        }
        require(result == VerificationState.Failed || failureSummary == null) {
            "Only failed verification results may include a failure summary."
        }
        require(result != VerificationState.Failed || !failureSummary.isNullOrBlank()) {
            "Failed verification results must include a failure summary."
        }
        PublicSafeTextPolicy.requirePublicSafe(name, fieldName = "Verification result name")
        PublicSafeTextPolicy.requirePublicSafe(outputReference, fieldName = "Verification output reference")
        failureSummary?.let {
            PublicSafeTextPolicy.requirePublicSafe(it, fieldName = "Verification failure summary")
        }
    }
}

data class CompletionEvidenceChecklist(
    val outcome: CompletionOutcome,
    val verificationAttempted: Boolean,
    val knownFailures: List<String>,
    val touchedArtifacts: List<String>,
    val residualRisks: List<String>,
    val verificationResults: List<VerificationResult>,
) {
    init {
        knownFailures.forEachIndexed { index, failure ->
            require(failure.isNotBlank()) { "Known failure must not be blank." }
            PublicSafeTextPolicy.requirePublicSafe(failure, fieldName = "Known failure[$index]")
        }
        touchedArtifacts.forEachIndexed { index, artifact ->
            require(artifact.isNotBlank()) { "Touched artifact must not be blank." }
            PublicSafeTextPolicy.requirePublicSafe(artifact, fieldName = "Touched artifact[$index]")
        }
        residualRisks.forEachIndexed { index, risk ->
            require(risk.isNotBlank()) { "Residual risk must not be blank." }
            PublicSafeTextPolicy.requirePublicSafe(risk, fieldName = "Residual risk[$index]")
        }
    }
}

data class CompletionReadinessIndicator(
    val state: CompletionReadinessState,
    val reasons: List<String>,
)

enum class VerificationKind {
    LocalTest,
    CiCheck,
    SmokeRun,
    ManualQa,
}

enum class VerificationState {
    Passed,
    Failed,
    Skipped,
    Unknown,
}

enum class VerificationFreshness {
    Fresh,
    Stale,
    Unknown,
}

enum class CompletionOutcome {
    Ready,
    NotReady,
    Blocked,
    Unknown,
}

enum class CompletionReadinessState {
    Ready,
    NotReady,
    Blocked,
    Unknown,
}

object CompletionEvidenceProjector {
    /**
     * Derives readiness from a checklist. Freshness is computed per result from
     * its input binding against [lastChangedAt] (the work item's last change),
     * not read from a caller-supplied claim — unbound or unverifiable evidence
     * is conservatively unknown and never contributes a fresh/passing signal.
     */
    fun readiness(
        checklist: CompletionEvidenceChecklist,
        lastChangedAt: EventTimestamp? = null,
    ): CompletionReadinessIndicator {
        val reasons = buildList {
            if (!checklist.verificationAttempted) {
                add("Verification was not attempted.")
            }
            if (checklist.verificationResults.isEmpty()) {
                add("No verification results were recorded.")
            }
            checklist.verificationResults
                .filter { it.result != VerificationState.Passed }
                .forEach { result ->
                    add("${result.name} is ${result.result.name.lowercase()}.")
                }
            checklist.verificationResults.forEach { result ->
                val freshness = VerificationFreshnessDeriver.derive(result.inputBinding, lastChangedAt)
                if (freshness != VerificationFreshness.Fresh) {
                    add("${result.name} freshness is ${freshness.name.lowercase()}.")
                }
            }
            checklist.knownFailures.forEach { failure ->
                add("Known failure: $failure")
            }
            checklist.residualRisks.forEach { risk ->
                add("Residual risk: $risk")
            }
        }

        val state = when {
            checklist.outcome == CompletionOutcome.Blocked -> CompletionReadinessState.Blocked
            checklist.outcome == CompletionOutcome.Unknown -> CompletionReadinessState.Unknown
            reasons.isNotEmpty() -> CompletionReadinessState.NotReady
            checklist.outcome == CompletionOutcome.Ready -> CompletionReadinessState.Ready
            else -> CompletionReadinessState.NotReady
        }

        return CompletionReadinessIndicator(
            state = state,
            reasons = reasons.ifEmpty { listOf("All recorded verification evidence is fresh and passing.") },
        )
    }
}
