package com.yonatankarp.agentdesk.app.security

import com.yonatankarp.agentdesk.app.fixtures.AppFixtures
import com.yonatankarp.agentdesk.app.operator.OperatorStatePresenter
import com.yonatankarp.agentdesk.app.operator.OperatorStateProjector
import com.yonatankarp.agentdesk.app.operator.verification.CompletionEvidenceChecklist
import com.yonatankarp.agentdesk.app.operator.verification.CompletionOutcome
import com.yonatankarp.agentdesk.app.persistence.WorkEventReadResult
import com.yonatankarp.agentdesk.app.persistence.WorkEventRepository
import com.yonatankarp.agentdesk.app.runtime.RuntimeWorkEventImportDiagnosticKind
import com.yonatankarp.agentdesk.app.runtime.RuntimeWorkEventImportException
import com.yonatankarp.agentdesk.app.runtime.RuntimeWorkEventImporter
import com.yonatankarp.agentdesk.app.runtime.RuntimeWorkEventSource
import com.yonatankarp.agentdesk.app.runtime.RuntimeWorkObservation
import com.yonatankarp.agentdesk.app.runtime.RuntimeWorkObservationKind
import com.yonatankarp.agentdesk.app.runtime.SanitizedRuntimeObservationMapper
import com.yonatankarp.agentdesk.app.runtime.summary
import com.yonatankarp.agentdesk.app.serialization.WorkEventJson
import com.yonatankarp.agentdesk.app.serialization.WorkEventPayloadRecord
import com.yonatankarp.agentdesk.app.serialization.WorkEventRecord
import com.yonatankarp.agentdesk.core.domain.events.EvidenceLabel
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReference
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReferenceKind
import com.yonatankarp.agentdesk.core.domain.events.EvidenceTarget
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class PrivacyBoundaryRegressionTest :
    BehaviorSpec({
        given("representative public-safe fixtures") {
            `when`("they move through runtime, persisted, UI, and report boundaries") {
                then("outputs remain publishable") {
                    val mapper = SanitizedRuntimeObservationMapper()
                    val events = listOf(
                        mapper.toWorkEvent(PrivacyFixtures.benignStartedRuntimeObservation),
                        mapper.toWorkEvent(PrivacyFixtures.benignRuntimeObservation),
                    )
                    val persisted = events.joinToString("\n", transform = WorkEventJson::encode)
                    val state = OperatorStateProjector.project(events)
                    val uiSummary = OperatorStatePresenter.eventLines(state).joinToString("\n") { line ->
                        "${line.type} ${line.workItemId} ${line.source} ${line.detail}"
                    }
                    val report = CompletionEvidenceChecklist(
                        outcome = CompletionOutcome.Ready,
                        verificationAttempted = true,
                        knownFailures = emptyList(),
                        touchedArtifacts = listOf("app/src/commonTest/kotlin/com/yonatankarp/agentdesk/app/security/PrivacyBoundaryRegressionTest.kt"),
                        residualRisks = emptyList(),
                        verificationResults = emptyList(),
                    )

                    assertPublicSafeOutput(persisted)
                    assertPublicSafeOutput(uiSummary)
                    report.touchedArtifacts shouldContain
                        "app/src/commonTest/kotlin/com/yonatankarp/agentdesk/app/security/PrivacyBoundaryRegressionTest.kt"
                }
            }
        }

        given("representative sensitive fixtures") {
            `when`("runtime observation fields contain private material") {
                then("the mapper rejects each sample without echoing it") {
                    PrivacyFixtures.runtimeSensitiveSamples.forEach { sample ->
                        val error = shouldThrow<IllegalArgumentException> {
                            SanitizedRuntimeObservationMapper().toWorkEvent(
                                PrivacyFixtures.benignRuntimeObservation.copy(reason = sample.value),
                            )
                        }

                        assertSoftly(error.message.orEmpty()) {
                            shouldContain("Runtime observation reason")
                            shouldNotContain(sample.value)
                        }
                    }
                }
            }

            `when`("import diagnostics are generated from unsafe observations") {
                then("they classify the rejection without private payloads") {
                    val sample = PrivacyFixtures.sample(PrivacyLeakKind.LocalPath)
                    val error = shouldThrow<RuntimeWorkEventImportException> {
                        RuntimeWorkEventImporter(
                            source = object : RuntimeWorkEventSource {
                                override fun loadObservations(): List<RuntimeWorkObservation> = listOf(PrivacyFixtures.benignRuntimeObservation.copy(reason = sample.value))
                            },
                            repository = InMemoryWorkEventRepository(),
                        ).importEvents()
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Runtime observations could not be imported.")
                        shouldNotContain(sample.value)
                    }
                    assertSoftly(error.diagnostics.single()) {
                        kind shouldBe RuntimeWorkEventImportDiagnosticKind.UnsafeRejected
                        message shouldBe "Runtime observation source was rejected."
                        eventId shouldBe null
                    }
                    error.diagnostics.summary().publicMessage() shouldBe
                        "Diagnostics: imported=0 skipped-duplicate=0 invalid=0 unsafe-rejected=1 store-rejected=0 redacted-or-dropped=0."
                }
            }

            `when`("persisted event records contain private material") {
                then("decoding rejects them without exposing the payload") {
                    val sample = PrivacyFixtures.sample(PrivacyLeakKind.PrivateUrl)
                    val error = shouldThrow<IllegalArgumentException> {
                        WorkEventJson.fromRecord(
                            WorkEventRecord(
                                id = "event:agent-task:410:blocked",
                                occurredAt = "2026-06-06T09:00:00Z",
                                source = "mock-adapter",
                                workItemId = "agent-task:410",
                                type = "work.blocked",
                                payload = WorkEventPayloadRecord(reason = sample.value),
                            ),
                        )
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Work summary")
                        shouldNotContain(sample.value)
                    }
                }
            }

            `when`("UI summaries would receive private material") {
                then("the domain boundary rejects before projection") {
                    val sample = PrivacyFixtures.sample(PrivacyLeakKind.RawChannelId)
                    val error = shouldThrow<IllegalArgumentException> {
                        OperatorStateProjector.project(
                            listOf(
                                AppFixtures.workStartedEvent().copy(
                                    evidenceReferences = listOf(
                                        EvidenceReference(
                                            kind = EvidenceReferenceKind.SanitizedNote,
                                            label = EvidenceLabel.parse("Unsafe UI evidence"),
                                            target = EvidenceTarget.parse(sample.value),
                                        ),
                                    ),
                                ),
                            ),
                        )
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Evidence target")
                        shouldNotContain(sample.value)
                    }
                }
            }

            `when`("generated completion reports include private material") {
                then("they are rejected without echoing the value") {
                    val sample = PrivacyFixtures.sample(PrivacyLeakKind.Credential)
                    val error = shouldThrow<IllegalArgumentException> {
                        CompletionEvidenceChecklist(
                            outcome = CompletionOutcome.Blocked,
                            verificationAttempted = true,
                            knownFailures = listOf(sample.value),
                            touchedArtifacts = emptyList(),
                            residualRisks = emptyList(),
                            verificationResults = emptyList(),
                        )
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Known failure")
                        shouldNotContain(sample.value)
                    }
                }
            }
        }
    })

private object PrivacyFixtures {
    val benignRuntimeObservation = RuntimeWorkObservation(
        eventId = "event:agent-task:410:blocked",
        occurredAt = "2026-06-06T09:00:00Z",
        source = "mock-adapter",
        workItemId = "agent-task:410",
        kind = RuntimeWorkObservationKind.Blocked,
        reason = "CI failed on the privacy boundary regression suite.",
    )

    val benignStartedRuntimeObservation = RuntimeWorkObservation(
        eventId = "event:agent-task:410:started",
        occurredAt = "2026-06-06T08:55:00Z",
        source = "mock-adapter",
        workItemId = "agent-task:410",
        kind = RuntimeWorkObservationKind.Started,
        title = "Run privacy boundary regression suite",
        summary = "Agent started privacy boundary regression checks.",
    )

    val sensitiveSamples = listOf(
        PrivacySample(PrivacyLeakKind.Token, listOf("auth", "token=value").joinToString("_")),
        PrivacySample(PrivacyLeakKind.Credential, listOf("password", "value").joinToString("=")),
        PrivacySample(PrivacyLeakKind.PrivateUrl, "https://" + listOf("localhost", "runtime").joinToString("/")),
        PrivacySample(PrivacyLeakKind.RawTranscriptMarker, listOf("raw", "transcript marker").joinToString(" ")),
        PrivacySample(PrivacyLeakKind.LocalPath, "/" + listOf("home", "operator", "private.log").joinToString("/")),
        PrivacySample(PrivacyLeakKind.RawChannelId, "123456789" + "012345678"),
        PrivacySample(PrivacyLeakKind.RuntimeSessionId, "session:local-agent"),
    )

    val runtimeSensitiveSamples = sensitiveSamples.filterNot { it.kind == PrivacyLeakKind.PrivateUrl }

    fun sample(kind: PrivacyLeakKind): PrivacySample = sensitiveSamples.single { it.kind == kind }
}

private data class PrivacySample(
    val kind: PrivacyLeakKind,
    val value: String,
)

private enum class PrivacyLeakKind {
    Token,
    Credential,
    PrivateUrl,
    RawTranscriptMarker,
    LocalPath,
    RawChannelId,
    RuntimeSessionId,
}

private class InMemoryWorkEventRepository : WorkEventRepository {
    private val events = mutableListOf<WorkEvent>()

    override fun append(event: WorkEvent) {
        events += event
    }

    override fun readAll(): WorkEventReadResult = WorkEventReadResult(events = events.toList())
}

private fun assertPublicSafeOutput(text: String) {
    assertSoftly(text) {
        shouldNotContain("/home/")
        shouldNotContain("/Users/")
        shouldNotContain("C:\\")
        shouldNotContain("localhost")
        shouldNotContain("auth_token")
        shouldNotContain("password")
        shouldNotContain("secret")
        shouldNotContain("token")
        shouldNotContain("raw transcript")
        shouldNotContain("channel:")
        shouldNotContain("session:")
    }
}
