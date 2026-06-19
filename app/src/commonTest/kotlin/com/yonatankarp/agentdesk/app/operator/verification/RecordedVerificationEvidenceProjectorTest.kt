package com.yonatankarp.agentdesk.app.operator.verification

import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.testfixtures.WorkEventFixtures
import com.yonatankarp.agentdesk.testfixtures.recordedVerificationInputBinding
import com.yonatankarp.agentdesk.testfixtures.recordedVerificationResult
import com.yonatankarp.agentdesk.testfixtures.workEvents
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class RecordedVerificationEvidenceProjectorTest :
    BehaviorSpec({
        given("recorded verification evidence") {
            `when`("work changes after the verification input was captured") {
                then("the live events-to-checklist path marks the result stale") {
                    val events = workEvents {
                        started()
                        verificationRecorded(
                            at = EventTimestamp.parse("2026-06-02T21:06:00Z"),
                            results = listOf(
                                recordedVerificationResult(
                                    inputBinding = recordedVerificationInputBinding(
                                        capturedAt = EventTimestamp.parse("2026-06-02T21:04:00Z"),
                                    ),
                                ),
                            ),
                        )
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
                    val events = workEvents {
                        started()
                        verificationRecorded(results = listOf(recordedVerificationResult(inputBinding = null)))
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
