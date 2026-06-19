package com.yonatankarp.agentdesk.core.domain.events

import com.yonatankarp.agentdesk.core.fixtures.CoreFixtures
import com.yonatankarp.agentdesk.testfixtures.checkRunEvidence
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class WorkEventTest :
    BehaviorSpec({
        given("a work started event") {
            `when`("the envelope type is read") {
                then("it exposes the payload type") {
                    val event = CoreFixtures.workStartedEvent()

                    event.type shouldBe WorkEventType.WorkStarted
                    event.type.wireName shouldBe "work.started"
                }
            }
        }

        given("a work blocked event") {
            `when`("the payload is inspected") {
                then("it carries the sanitized reason") {
                    val event = CoreFixtures.workBlockedEvent()

                    event.type shouldBe WorkEventType.WorkBlocked
                    event.payload shouldBe WorkBlockedPayload(reason = CoreFixtures.blockedReason)
                }
            }
        }

        given("additional lifecycle events") {
            `when`("their envelope types are read") {
                then("they expose stable wire names") {
                    CoreFixtures.workNeedsDecisionEvent().type.wireName shouldBe "work.needs-decision"
                    CoreFixtures.workSucceededEvent().type.wireName shouldBe "work.succeeded"
                    CoreFixtures.workFailedEvent().type.wireName shouldBe "work.failed"
                    CoreFixtures.workCanceledEvent().type.wireName shouldBe "work.canceled"
                }
            }
        }

        given("verification recorded events") {
            `when`("the payload is inspected") {
                then("it carries only public-safe verification facts") {
                    val payload = verificationRecordedPayload()

                    payload.type shouldBe WorkEventType.WorkVerificationRecorded
                    payload.outcome shouldBe WorkVerificationOutcome.Ready
                    payload.results.single().kind shouldBe RecordedVerificationKind.LocalTest
                    payload.results.single().result shouldBe RecordedVerificationState.Passed
                    payload.results.single().inputBinding?.algorithm shouldBe RecordedDigestAlgorithm.Sha256
                }
            }

            `when`("the recorded digest is parsed") {
                then("it accepts only lowercase SHA-256 hex") {
                    RecordedContentDigest.parseSha256(
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    ).toString() shouldBe "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"

                    shouldThrow<IllegalArgumentException> {
                        RecordedContentDigest.parseSha256("A".repeat(64))
                    }.message shouldContain "SHA-256"
                }
            }

            `when`("a failed result omits its failure summary") {
                then("it rejects the payload") {
                    shouldThrow<IllegalArgumentException> {
                        RecordedVerificationResult(
                            name = CoreFixtures.startedSummary,
                            kind = RecordedVerificationKind.LocalTest,
                            result = RecordedVerificationState.Failed,
                            durationMillis = 1,
                            outputReference = CoreFixtures.startedSummary,
                            evidenceReference = checkRunEvidence(
                                "Gradle Build",
                                "https://github.com/yonatankarp/agent-desk/actions/runs/26937983933",
                            ),
                        )
                    }.message shouldContain "Failed verification results"
                }
            }
        }

        given("event identifiers and sources") {
            `when`("raw values use surrounding whitespace and mixed case") {
                then("they normalize case") {
                    WorkEventId.parse("  Event:Agent-Task:42:Started  ").value shouldBe
                        "event:agent-task:42:started"
                    EventSource.parse("  Mock-Adapter  ").value shouldBe "mock-adapter"
                }
            }

            `when`("raw values include unsupported characters") {
                then("they reject the values") {
                    shouldThrow<IllegalArgumentException> {
                        WorkEventId.parse("event agent task")
                    }
                    shouldThrow<IllegalArgumentException> {
                        EventSource.parse("mock adapter")
                    }
                }
            }

            `when`("event ids include raw runtime identifiers") {
                then("they reject them without echoing unsafe values") {
                    val rawIdentifier = "123456789" + "012345678"
                    val messageEventId = "event:message:$rawIdentifier:started"
                    val channelSource = "channel:$rawIdentifier"
                    val messageSource = "message:$rawIdentifier"
                    val sessionSource = "session:local-agent"
                    val threadSource = "thread:public-review"

                    val rawError = shouldThrow<IllegalArgumentException> {
                        WorkEventId.parse("event:agent-task:$rawIdentifier:started")
                    }
                    val messageError = shouldThrow<IllegalArgumentException> {
                        WorkEventId.parse(messageEventId)
                    }
                    val channelSourceError = shouldThrow<IllegalArgumentException> {
                        EventSource.parse(channelSource)
                    }
                    val messageSourceError = shouldThrow<IllegalArgumentException> {
                        EventSource.parse(messageSource)
                    }
                    val sessionSourceError = shouldThrow<IllegalArgumentException> {
                        EventSource.parse(sessionSource)
                    }
                    val threadSourceError = shouldThrow<IllegalArgumentException> {
                        EventSource.parse(threadSource)
                    }

                    assertSoftly {
                        rawError.message shouldContain "raw channel or message identifiers"
                        rawError.message.orEmpty() shouldNotContain rawIdentifier
                        messageError.message shouldContain "private runtime"
                        messageError.message.orEmpty() shouldNotContain messageEventId
                        channelSourceError.message shouldContain "private runtime"
                        channelSourceError.message.orEmpty() shouldNotContain channelSource
                        channelSourceError.message.orEmpty() shouldNotContain rawIdentifier
                        messageSourceError.message shouldContain "private runtime"
                        messageSourceError.message.orEmpty() shouldNotContain messageSource
                        messageSourceError.message.orEmpty() shouldNotContain rawIdentifier
                        sessionSourceError.message shouldContain "private runtime"
                        sessionSourceError.message.orEmpty() shouldNotContain sessionSource
                        threadSourceError.message shouldContain "private runtime"
                        threadSourceError.message.orEmpty() shouldNotContain threadSource
                    }
                }
            }
        }

        given("event timestamps") {
            `when`("raw values are not UTC instants") {
                then("they reject the values") {
                    shouldThrow<IllegalArgumentException> {
                        EventTimestamp.parse("2026-06-02 21:00:00")
                    }

                    shouldThrow<IllegalArgumentException> {
                        EventTimestamp.parse("2026-06-02T21:00:00+02:00")
                    }
                }
            }
        }

        given("evidence references") {
            `when`("public-safe evidence is parsed") {
                then("it preserves adapter-neutral kind, label, and target") {
                    val reference = checkRunEvidence(
                        "Gradle Build",
                        "https://github.com/yonatankarp/agent-desk/actions/runs/26937983933",
                    )

                    reference.kind.wireName shouldBe "check-run"
                    reference.label.toString() shouldBe "Gradle Build"
                    reference.target.toString() shouldBe
                        "https://github.com/yonatankarp/agent-desk/actions/runs/26937983933"
                }
            }

            `when`("evidence points at private or unsafe material") {
                then("it rejects the reference text") {
                    shouldThrow<IllegalArgumentException> {
                        EvidenceTarget.parse("/" + "home/yonatan/.openclaw/private.log")
                    }.message shouldContain "private local paths"

                    shouldThrow<IllegalArgumentException> {
                        EvidenceTarget.parse("channel:" + "151144681" + "8880225483")
                    }.message shouldContain "private runtime"

                    shouldThrow<IllegalArgumentException> {
                        EvidenceLabel.parse("raw transcript excerpt")
                    }.message shouldContain "private runtime"

                    unsafeEvidenceLabels().forEach { unsafeLabel ->
                        val error = shouldThrow<IllegalArgumentException> {
                            EvidenceLabel.parse(unsafeLabel)
                        }

                        assertSoftly(error.message.orEmpty()) {
                            shouldContain("Evidence label")
                            shouldNotContain(unsafeLabel)
                        }
                    }

                    shouldThrow<IllegalArgumentException> {
                        EvidenceTarget.parse("https://localhost:8080/report")
                    }.message shouldContain "private hosts"

                    unsafeEvidenceUrls().forEach { unsafeUrl ->
                        val error = shouldThrow<IllegalArgumentException> {
                            EvidenceTarget.parse(unsafeUrl)
                        }

                        assertSoftly(error.message.orEmpty()) {
                            shouldContain("Evidence target")
                            shouldNotContain(unsafeUrl)
                        }
                    }

                    shouldThrow<IllegalArgumentException> {
                        EvidenceTarget.parse("github_pat_1234567890")
                    }.message shouldContain "credentials"
                }
            }
        }
    })

private fun verificationRecordedPayload(): WorkVerificationRecordedPayload = WorkVerificationRecordedPayload(
    outcome = WorkVerificationOutcome.Ready,
    verificationAttempted = true,
    knownFailures = emptyList(),
    touchedArtifacts = listOf(CoreFixtures.startedSummary),
    residualRisks = emptyList(),
    results = listOf(
        RecordedVerificationResult(
            name = CoreFixtures.startedSummary,
            kind = RecordedVerificationKind.LocalTest,
            result = RecordedVerificationState.Passed,
            durationMillis = 1,
            outputReference = CoreFixtures.startedSummary,
            evidenceReference = checkRunEvidence(
                "Gradle Build",
                "https://github.com/yonatankarp/agent-desk/actions/runs/26937983933",
            ),
            inputBinding = RecordedVerificationInputBinding(
                digest = RecordedContentDigest.parseSha256(
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                ),
                algorithm = RecordedDigestAlgorithm.Sha256,
                capturedAt = CoreFixtures.terminalAt,
            ),
        ),
    ),
)

private fun unsafeEvidenceLabels(): List<String> {
    val rawIdentifier = "123456789" + "012345678"
    val unixPath = "/" + listOf("home", "operator", "workspace", "private.log").joinToString("/")

    return listOf(
        rawIdentifier,
        "channel:$rawIdentifier",
        "message:$rawIdentifier",
        "session:local-agent",
        "thread:local-review",
        "agent:main:worker",
        "[subagent worker]",
        "<conversation worker>",
        unixPath,
        listOf("raw", "transcript excerpt").joinToString(" "),
        listOf("Open", "Claw runtime context").joinToString(""),
        listOf("auth", "token=value").joinToString("_"),
        "github_pat_marker",
        "ghp_marker",
        "op://vault/item",
        "password=value",
        "secret=value",
        "xoxb-marker",
    )
}

private fun unsafeEvidenceUrls(): List<String> {
    val rawIdentifier = "169" + ".254.169.254"

    return listOf(
        "http://github.com/yonatankarp/agent-desk/actions/runs/26937983933",
        "https://github.com@127.0.0.1/report",
        "https://trusted.example@$rawIdentifier/latest",
        "https://user:pass@github.com/yonatankarp/agent-desk",
        "https://127.0.0.1/report",
        "https://127.1/report",
        "https://10.0.0.1/report",
        "https://10.1/report",
        "https://172.16.0.1/report",
        "https://172.16.1/report",
        "https://192.168.1.20/report",
        "https://$rawIdentifier/latest",
        "https://0.0.0.0/report",
        "https://224.0.0.1/report",
        "https://[::1]/report",
        "https://[::]/report",
        "https://[::0001]/report",
        "https://[::01]/report",
        "https://[::0000]/report",
        "https://[0:0:0:0:0:0:0:0001]/report",
        "https://[::ffff:127.0.0.1]/report",
        "https://[fe80::1]/report",
        "https://[fc00::1]/report",
        "https://[ff02::1]/report",
        "https://localhost/report",
        "https://localhost.localdomain/report",
        "https://agent.local/report",
        "https://foo.localhost/report",
    )
}
