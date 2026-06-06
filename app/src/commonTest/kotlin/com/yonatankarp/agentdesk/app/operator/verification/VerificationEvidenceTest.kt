package com.yonatankarp.agentdesk.app.operator.verification

import com.yonatankarp.agentdesk.core.domain.events.EvidenceLabel
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReference
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReferenceKind
import com.yonatankarp.agentdesk.core.domain.events.EvidenceTarget
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class VerificationEvidenceTest :
    BehaviorSpec({
        given("fresh passing evidence") {
            `when`("a completion checklist is projected") {
                then("it becomes ready") {
                    val checklist = CompletionEvidenceChecklist(
                        outcome = CompletionOutcome.Ready,
                        verificationAttempted = true,
                        knownFailures = emptyList(),
                        touchedArtifacts = listOf(
                            "app/src/commonMain/kotlin/com/yonatankarp/agentdesk/app/operator/verification/VerificationEvidence.kt",
                        ),
                        residualRisks = emptyList(),
                        verificationResults = listOf(
                            verification(
                                name = "app verification tests",
                                kind = VerificationKind.LocalTest,
                                result = VerificationState.Passed,
                            ),
                            verification(
                                name = "CI Gradle Build",
                                kind = VerificationKind.CiCheck,
                                result = VerificationState.Passed,
                                outputReference = "github-actions:gradle-build",
                            ),
                            verification(
                                name = "mock runtime smoke",
                                kind = VerificationKind.SmokeRun,
                                result = VerificationState.Passed,
                                outputReference = "artifact:mock-runtime-smoke",
                            ),
                            verification(
                                name = "manual QA review",
                                kind = VerificationKind.ManualQa,
                                result = VerificationState.Passed,
                                outputReference = "sanitized-note:manual-qa",
                            ),
                        ),
                    )

                    val readiness = CompletionEvidenceProjector.readiness(checklist)

                    readiness.state shouldBe CompletionReadinessState.Ready
                    readiness.reasons shouldContain "All recorded verification evidence is fresh and passing."
                }
            }
        }

        given("failed, skipped, stale, and unknown verification") {
            `when`("readiness is projected") {
                then("it explains why completion is not ready") {
                    val checklist = CompletionEvidenceChecklist(
                        outcome = CompletionOutcome.Ready,
                        verificationAttempted = true,
                        knownFailures = listOf("Windows Compose Build failed."),
                        touchedArtifacts = listOf("desktop/src/jvmMain/kotlin/com/yonatankarp/agentdesk/desktop/AgentDeskApp.kt"),
                        residualRisks = listOf("Manual screenshot evidence is stale."),
                        verificationResults = listOf(
                            verification(
                                name = "Windows Compose Build",
                                kind = VerificationKind.CiCheck,
                                result = VerificationState.Failed,
                                failureSummary = "Gradle task failed.",
                            ),
                            verification(
                                name = "mobile smoke",
                                kind = VerificationKind.SmokeRun,
                                result = VerificationState.Skipped,
                            ),
                            verification(
                                name = "manual QA review",
                                kind = VerificationKind.ManualQa,
                                result = VerificationState.Passed,
                                freshness = VerificationFreshness.Stale,
                            ),
                            verification(
                                name = "coverage report",
                                kind = VerificationKind.CiCheck,
                                result = VerificationState.Unknown,
                                freshness = VerificationFreshness.Unknown,
                            ),
                        ),
                    )

                    val readiness = CompletionEvidenceProjector.readiness(checklist)

                    assertSoftly(readiness) {
                        state shouldBe CompletionReadinessState.NotReady
                        reasons shouldContain "Windows Compose Build is failed."
                        reasons shouldContain "mobile smoke is skipped."
                        reasons shouldContain "coverage report is unknown."
                        reasons shouldContain "manual QA review freshness is stale."
                        reasons shouldContain "coverage report freshness is unknown."
                        reasons shouldContain "Known failure: Windows Compose Build failed."
                        reasons shouldContain "Residual risk: Manual screenshot evidence is stale."
                    }
                }
            }
        }

        given("blocked and unattempted verification") {
            `when`("readiness is projected") {
                then("blocked outcome wins and explains missing verification") {
                    val readiness = CompletionEvidenceProjector.readiness(
                        CompletionEvidenceChecklist(
                            outcome = CompletionOutcome.Blocked,
                            verificationAttempted = false,
                            knownFailures = emptyList(),
                            touchedArtifacts = emptyList(),
                            residualRisks = emptyList(),
                            verificationResults = emptyList(),
                        ),
                    )

                    readiness.state shouldBe CompletionReadinessState.Blocked
                    readiness.reasons shouldContain "Verification was not attempted."
                    readiness.reasons shouldContain "No verification results were recorded."
                }
            }
        }

        given("validation") {
            `when`("a failed result omits failure summary") {
                then("it is rejected") {
                    val error = shouldThrow<IllegalArgumentException> {
                        verification(
                            name = "failing check",
                            kind = VerificationKind.LocalTest,
                            result = VerificationState.Failed,
                            failureSummary = null,
                        )
                    }

                    error.message.orEmpty() shouldContain "Failed verification results must include a failure summary"
                }
            }

            `when`("unsafe text is provided") {
                then("it rejects without echoing the unsafe value") {
                    val unsafeName = "Read " + "/" + "home/user/private.log"

                    val error = shouldThrow<IllegalArgumentException> {
                        verification(
                            name = unsafeName,
                            kind = VerificationKind.LocalTest,
                            result = VerificationState.Passed,
                        )
                    }

                    error.message.orEmpty() shouldContain "Verification result name"
                    error.message.orEmpty() shouldNotContain unsafeName
                }
            }
        }
    })

private fun verification(
    name: String,
    kind: VerificationKind,
    result: VerificationState,
    durationMillis: Long? = 1_200,
    outputReference: String = "artifact:verification-output",
    failureSummary: String? = null,
    freshness: VerificationFreshness = VerificationFreshness.Fresh,
): VerificationResult = VerificationResult(
    name = name,
    kind = kind,
    result = result,
    durationMillis = durationMillis,
    outputReference = outputReference,
    failureSummary = failureSummary,
    freshness = freshness,
    evidenceReference = EvidenceReference(
        kind = EvidenceReferenceKind.SanitizedNote,
        label = EvidenceLabel.parse("Verification evidence"),
        target = EvidenceTarget.parse(outputReference),
    ),
)
