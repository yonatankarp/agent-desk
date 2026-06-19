package com.yonatankarp.agentdesk.app.operator.verification

import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.RecordedContentDigest
import com.yonatankarp.agentdesk.core.domain.events.RecordedDigestAlgorithm
import com.yonatankarp.agentdesk.core.domain.events.RecordedVerificationInputBinding
import com.yonatankarp.agentdesk.core.domain.events.RecordedVerificationKind
import com.yonatankarp.agentdesk.core.domain.events.RecordedVerificationResult
import com.yonatankarp.agentdesk.core.domain.events.RecordedVerificationState
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.events.WorkVerificationOutcome
import com.yonatankarp.agentdesk.core.domain.events.WorkVerificationRecordedPayload
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary
import com.yonatankarp.agentdesk.testfixtures.WorkEventFixtures
import com.yonatankarp.agentdesk.testfixtures.checkRunEvidence
import com.yonatankarp.agentdesk.testfixtures.workEvents
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class RecordedVerificationEvidenceProjectorTest :
    BehaviorSpec({
        given("recorded verification evidence") {
            `when`("work changes after the verification input was captured") {
                then("the live events-to-checklist path marks the result stale") {
                    val verification = verificationRecordedEvent(
                        capturedAt = EventTimestamp.parse("2026-06-02T21:04:00Z"),
                        occurredAt = EventTimestamp.parse("2026-06-02T21:06:00Z"),
                    )
                    val events = workEvents {
                        started()
                        event(verification)
                        blocked(at = EventTimestamp.parse("2026-06-02T21:08:00Z"))
                    }

                    val evidence = RecordedVerificationEvidenceProjector.project(events, WorkEventFixtures.workItemId)
                    val readiness = CompletionEvidenceProjector.readiness(evidence.checklist, evidence.lastChangedAt)

                    evidence.checklist.verificationResults.single().name shouldBe "Gradle check"
                    readiness.reasons shouldContain "Gradle check freshness is stale."
                }
            }

            `when`("a result has no input binding") {
                then("the live events-to-checklist path keeps freshness unknown") {
                    val verification = verificationRecordedEvent(inputBinding = null)
                    val events = workEvents {
                        started()
                        event(verification)
                    }

                    val evidence = RecordedVerificationEvidenceProjector.project(events, WorkEventFixtures.workItemId)
                    val readiness = CompletionEvidenceProjector.readiness(evidence.checklist, evidence.lastChangedAt)

                    readiness.reasons shouldContain "Gradle check freshness is unknown."
                }
            }

            `when`("no verification evidence exists") {
                then("the empty evidence projection stays unknown") {
                    val events = workEvents { started() }

                    val evidence = RecordedVerificationEvidenceProjector.project(events, WorkEventFixtures.workItemId)
                    val readiness = CompletionEvidenceProjector.readiness(evidence.checklist, evidence.lastChangedAt)

                    readiness.state shouldBe CompletionReadinessState.Unknown
                    readiness.reasons shouldContain "Verification was not attempted."
                    readiness.reasons shouldContain "No verification results were recorded."
                }
            }
        }
    })

private fun verificationRecordedEvent(
    occurredAt: EventTimestamp = EventTimestamp.parse("2026-06-02T21:06:00Z"),
    capturedAt: EventTimestamp = EventTimestamp.parse("2026-06-02T21:06:00Z"),
    inputBinding: RecordedVerificationInputBinding? = recordedInputBinding(capturedAt),
): WorkEvent = WorkEvent(
    id = WorkEventId.parse("event:agent-task:42:verification-recorded"),
    occurredAt = occurredAt,
    source = WorkEventFixtures.eventSource,
    workItemId = WorkEventFixtures.workItemId,
    payload = WorkVerificationRecordedPayload(
        outcome = WorkVerificationOutcome.Ready,
        verificationAttempted = true,
        knownFailures = emptyList(),
        touchedArtifacts = listOf(WorkSummary.parse("app/src/commonMain/kotlin/com/yonatankarp/agentdesk/app/operator/verification/VerificationEvidence.kt")),
        residualRisks = emptyList(),
        results = listOf(
            RecordedVerificationResult(
                name = WorkSummary.parse("Gradle check"),
                kind = RecordedVerificationKind.LocalTest,
                result = RecordedVerificationState.Passed,
                durationMillis = 1200,
                outputReference = WorkSummary.parse("checks/gradle-check"),
                failureSummary = null,
                evidenceReference = checkRunEvidence("Gradle check", "https://github.com/yonatankarp/agent-desk/actions/runs/27793545211"),
                inputBinding = inputBinding,
            ),
        ),
    ),
)

private fun recordedInputBinding(capturedAt: EventTimestamp): RecordedVerificationInputBinding = RecordedVerificationInputBinding(
    digest = RecordedContentDigest.parseSha256("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
    algorithm = RecordedDigestAlgorithm.Sha256,
    capturedAt = capturedAt,
)
