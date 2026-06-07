package com.yonatankarp.agentdesk.app.operator.verification

import com.yonatankarp.agentdesk.app.fixtures.verificationBinding
import com.yonatankarp.agentdesk.app.fixtures.verificationResult
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private val LAST_CHANGED_AT = EventTimestamp.parse("2026-06-02T21:10:00Z")

class VerificationEvidenceTest :
    BehaviorSpec({
        given("passing evidence bound to content verified after the last change") {
            `when`("a completion checklist is projected") {
                then("it becomes ready") {
                    val freshBinding = verificationBinding(capturedAtMinute = 20)
                    val checklist = CompletionEvidenceChecklist(
                        outcome = CompletionOutcome.Ready,
                        verificationAttempted = true,
                        knownFailures = emptyList(),
                        touchedArtifacts = listOf(
                            "app/src/commonMain/kotlin/com/yonatankarp/agentdesk/app/operator/verification/VerificationEvidence.kt",
                        ),
                        residualRisks = emptyList(),
                        verificationResults = listOf(
                            verificationResult(
                                name = "app verification tests",
                                kind = VerificationKind.LocalTest,
                                result = VerificationState.Passed,
                                inputBinding = freshBinding,
                            ),
                            verificationResult(
                                name = "CI Gradle Build",
                                kind = VerificationKind.CiCheck,
                                result = VerificationState.Passed,
                                outputReference = "github-actions:gradle-build",
                                inputBinding = freshBinding,
                            ),
                        ),
                    )

                    val readiness = CompletionEvidenceProjector.readiness(checklist, lastChangedAt = LAST_CHANGED_AT)

                    readiness.state shouldBe CompletionReadinessState.Ready
                    readiness.reasons shouldContain "All recorded verification evidence is fresh and passing."
                }
            }
        }

        given("evidence captured at the exact instant of the last change") {
            `when`("readiness is projected") {
                then("the inclusive boundary derives fresh, keeping it ready") {
                    val checklist = CompletionEvidenceChecklist(
                        outcome = CompletionOutcome.Ready,
                        verificationAttempted = true,
                        knownFailures = emptyList(),
                        touchedArtifacts = emptyList(),
                        residualRisks = emptyList(),
                        verificationResults = listOf(
                            verificationResult(
                                name = "app verification tests",
                                kind = VerificationKind.LocalTest,
                                result = VerificationState.Passed,
                                inputBinding = verificationBinding(capturedAtMinute = 10),
                            ),
                        ),
                    )

                    val readiness = CompletionEvidenceProjector.readiness(checklist, lastChangedAt = LAST_CHANGED_AT)

                    readiness.state shouldBe CompletionReadinessState.Ready
                }
            }
        }

        given("a passing result that claims no immutable input binding") {
            `when`("readiness is projected") {
                then("the unbound evidence is treated as unknown freshness, never fresh") {
                    val checklist = CompletionEvidenceChecklist(
                        outcome = CompletionOutcome.Ready,
                        verificationAttempted = true,
                        knownFailures = emptyList(),
                        touchedArtifacts = emptyList(),
                        residualRisks = emptyList(),
                        verificationResults = listOf(
                            verificationResult(
                                name = "app verification tests",
                                kind = VerificationKind.LocalTest,
                                result = VerificationState.Passed,
                                inputBinding = null,
                            ),
                        ),
                    )

                    val readiness = CompletionEvidenceProjector.readiness(checklist, lastChangedAt = LAST_CHANGED_AT)

                    assertSoftly(readiness) {
                        state shouldBe CompletionReadinessState.NotReady
                        reasons shouldContain "app verification tests freshness is unknown."
                    }
                }
            }
        }

        given("evidence whose input was captured before the last change") {
            `when`("readiness is projected") {
                then("freshness is derived as stale, not the caller's claim") {
                    val checklist = CompletionEvidenceChecklist(
                        outcome = CompletionOutcome.Ready,
                        verificationAttempted = true,
                        knownFailures = listOf("Windows Compose Build failed."),
                        touchedArtifacts = listOf("desktop/src/jvmMain/kotlin/com/yonatankarp/agentdesk/desktop/AgentDeskApp.kt"),
                        residualRisks = listOf("Manual screenshot evidence is stale."),
                        verificationResults = listOf(
                            verificationResult(
                                name = "Windows Compose Build",
                                kind = VerificationKind.CiCheck,
                                result = VerificationState.Failed,
                                failureSummary = "Gradle task failed.",
                            ),
                            verificationResult(
                                name = "mobile smoke",
                                kind = VerificationKind.SmokeRun,
                                result = VerificationState.Skipped,
                            ),
                            verificationResult(
                                name = "manual QA review",
                                kind = VerificationKind.ManualQa,
                                result = VerificationState.Passed,
                                inputBinding = verificationBinding(capturedAtMinute = 5),
                            ),
                            verificationResult(
                                name = "coverage report",
                                kind = VerificationKind.CiCheck,
                                result = VerificationState.Unknown,
                            ),
                        ),
                    )

                    val readiness = CompletionEvidenceProjector.readiness(checklist, lastChangedAt = LAST_CHANGED_AT)

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
                        lastChangedAt = LAST_CHANGED_AT,
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
                        verificationResult(
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
                        verificationResult(
                            name = unsafeName,
                            kind = VerificationKind.LocalTest,
                            result = VerificationState.Passed,
                        )
                    }

                    error.message.orEmpty() shouldContain "Verification result name"
                    error.message.orEmpty() shouldNotContain unsafeName
                }
            }

            `when`("a content digest is not a 64-character lowercase hex string") {
                then("it is rejected without echoing the value") {
                    assertSoftly {
                        shouldThrow<IllegalArgumentException> { ContentDigest.parseSha256("") }
                        shouldThrow<IllegalArgumentException> { ContentDigest.parseSha256("XYZ") }
                        shouldThrow<IllegalArgumentException> { ContentDigest.parseSha256("A".repeat(64)) }
                        shouldThrow<IllegalArgumentException> { ContentDigest.parseSha256("a".repeat(63)) }
                    }
                }
            }
        }
    })
