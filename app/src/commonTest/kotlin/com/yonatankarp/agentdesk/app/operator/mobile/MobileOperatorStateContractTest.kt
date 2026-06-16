package com.yonatankarp.agentdesk.app.operator.mobile

import com.yonatankarp.agentdesk.app.fixtures.AppFixtures
import com.yonatankarp.agentdesk.app.operator.StatusTone
import com.yonatankarp.agentdesk.app.serialization.WorkEventJson
import com.yonatankarp.agentdesk.core.domain.events.ProvenanceId
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.events.WorkProvenance
import com.yonatankarp.agentdesk.testfixtures.checkRunEvidence
import com.yonatankarp.agentdesk.testfixtures.eventTimestampAt
import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import com.yonatankarp.agentdesk.testfixtures.sanitizedNoteEvidence
import com.yonatankarp.agentdesk.testfixtures.workEvents
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class MobileOperatorStateContractTest :
    BehaviorSpec({
        given("sample operator state") {
            `when`("the mobile contract is derived") {
                then("it exposes current work and human attention without adapter details") {
                    val state = MobileOperatorStateContract.sample()

                    assertSoftly {
                        state.currentWork.map { it.id }.shouldContainExactly(
                            "agent-task:42",
                            "agent-task:43",
                            "agent-task:44",
                        )
                        state.attentionQueue.map { it.workItem.id }.shouldContainExactly("agent-task:43", "agent-task:44")
                        state.attentionQueue.map { it.workItem.status.label }
                            .shouldContainExactly("Needs decision", "Blocked")
                        state.projectionWarnings shouldBe emptyList()
                    }
                }
            }
        }

        given("stored events with attention and evidence") {
            `when`("the mobile contract is derived from events") {
                then("it preserves status presentation and compact public-safe evidence references") {
                    val state = MobileOperatorStateContract.fromEvents(
                        workEvents {
                            started()
                            needsDecision(
                                evidence = listOf(
                                    checkRunEvidence(
                                        "Mobile contract check",
                                        "https://github.com/yonatankarp/agent-desk/actions/runs/26937983933",
                                    ),
                                ),
                            )
                        },
                    )

                    assertSoftly {
                        state.currentWork.single().status shouldBe
                            MobileStatusPresentation(label = "Needs decision", tone = StatusTone.Attention)
                        state.currentWork.single().evidenceReferences.single() shouldBe
                            MobileEvidenceReference(
                                kind = "check-run",
                                label = "Mobile contract check",
                                target = "https://github.com/yonatankarp/agent-desk/actions/runs/26937983933",
                            )
                        state.attentionQueue.single().reason shouldBe "Operator decision needed."
                        state.recentEvents.last().evidenceReferences.single().kind shouldBe "check-run"
                    }
                }
            }
        }

        given("stored events with stale running work") {
            `when`("a newer accepted event is past the stale threshold") {
                then("stale attention is included in the mobile attention queue") {
                    val state = MobileOperatorStateContract.fromEvents(
                        workEvents {
                            started()
                            started(
                                workItemId = "agent-task:77",
                                at = eventTimestampAt(minute = 1, hour = 22),
                                title = "Refresh operator summary",
                                summary = "Agent started a later task.",
                            )
                        },
                    )

                    val stale = state.attentionQueue.single { it.workItem.id == "agent-task:42" }

                    assertSoftly {
                        stale.workItem.status.label shouldBe "Running"
                        stale.stale shouldBe MobileStaleAttention(
                            lastEventAt = "2026-06-02T21:00:00Z",
                            staleForMinutes = 61,
                        )
                    }
                }
            }
        }

        given("stored events with a projection warning") {
            `when`("an invalid transition follows accepted state") {
                then("accepted current work and public-safe warning details are both exposed") {
                    val state = MobileOperatorStateContract.fromEvents(
                        workEvents {
                            started()
                            succeeded()
                            event(
                                AppFixtures.workBlockedEvent(
                                    id = WorkEventId.parse("event:agent-task:42:blocked-after-success"),
                                ),
                            )
                        },
                    )

                    assertSoftly {
                        state.currentWork shouldBe emptyList()
                        state.projectionWarnings.single() shouldBe
                            MobileProjectionWarning(
                                eventId = "event:agent-task:42:blocked-after-success",
                                reason = "Cannot transition work item agent-task:42 from Succeeded to Blocked",
                            )
                    }
                }
            }
        }

        given("stored events for the read-only timeline") {
            `when`("the mobile contract is derived from events") {
                then("timeline entries carry source, state labels, and status markers") {
                    val state = MobileOperatorStateContract.fromEvents(
                        workEvents {
                            started()
                            blocked(evidence = listOf(sanitizedNoteEvidence("Blocked context", "docs/blocked-context.md")))
                        },
                    )

                    val entry = state.timeline.last()
                    assertSoftly {
                        state.timeline shouldHaveSize 2
                        entry.source shouldBe "mock-adapter"
                        entry.type shouldBe "work.blocked"
                        entry.stateLabel shouldBe "Blocked"
                        entry.statusLabel shouldBe "Blocked"
                        entry.timeWindow shouldBe "2026-06-02"
                        entry.evidenceReferences.single().target shouldBe "docs/blocked-context.md"
                        state.timelineStatusMarkers.shouldContainExactly("Read-only", "Blocked")
                        state.recentEvents.last().source shouldBe "mock-adapter"
                    }
                }
            }
        }

        given("stored events with provenance") {
            `when`("the mobile contract is derived from events") {
                then("timeline and evidence detail expose structured public-safe provenance") {
                    val provenance = WorkProvenance(
                        projectId = ProvenanceId.parse("project:agent-desk"),
                        workspaceId = ProvenanceId.parse("workspace:mobile"),
                        sourceId = ProvenanceId.parse("repo:agent-desk"),
                        ownerId = ProvenanceId.parse("owner:local"),
                        agentId = ProvenanceId.parse("agent:ororo"),
                        modelId = ProvenanceId.parse("model:gpt-5"),
                        toolId = ProvenanceId.parse("tool:gradle"),
                        runId = ProvenanceId.parse("run:daily-20260616"),
                        objectiveId = ProvenanceId.parse("objective:provenance"),
                        parentHandoffId = ProvenanceId.parse("handoff:manager"),
                        archiveRecordId = ProvenanceId.parse("archive:agent-task-42"),
                    )
                    val state = MobileOperatorStateContract.fromEvents(
                        workEvents {
                            started(provenance = provenance)
                        },
                    )

                    assertSoftly {
                        state.recentEvents.single().provenance?.projectId shouldBe "project:agent-desk"
                        state.timeline.single().provenance?.sourceId shouldBe "repo:agent-desk"
                        state.timeline.single().provenance?.agentId shouldBe "agent:ororo"
                        state.evidenceDetails.single().provenanceFields?.modelId shouldBe "model:gpt-5"
                        state.evidenceDetails.single().provenanceFields?.archiveRecordId shouldBe "archive:agent-task-42"
                        state.evidenceDetails.single().provenanceFields.toString().shouldBePublicSafe()
                    }
                }
            }
        }

        given("stored events for evidence detail") {
            `when`("the mobile contract is derived from events") {
                then("the detail exposes source, timestamp, summary, provenance, and related items") {
                    val state = MobileOperatorStateContract.fromEvents(
                        workEvents {
                            started()
                            blocked(evidence = listOf(sanitizedNoteEvidence("Blocked context", "docs/blocked-context.md")))
                        },
                    )

                    val detail = state.evidenceDetails.last()
                    assertSoftly {
                        detail.source shouldBe "mock-adapter"
                        detail.timestamp shouldBe state.timeline.last().occurredAt
                        detail.summary shouldBe state.timeline.last().summary
                        detail.provenance shouldBe "replay event event:agent-task:42:blocked"
                        detail.evidenceReferences.single().target shouldBe "docs/blocked-context.md"
                        detail.relatedEvents.single().type shouldBe "work.started"
                        detail.decisionState shouldBe null
                        detail.decisionUnavailableReason shouldBe "unavailable for latest replay event."
                        detail.criteriaResult shouldBe "Not done: 1 item(s) need operator attention."
                    }
                }

                then("the detail exposes recorded decision state and criteria result") {
                    val state = MobileOperatorStateContract.fromEvents(
                        workEvents {
                            started()
                            needsDecision(evidence = listOf(sanitizedNoteEvidence("Decision context", "docs/decision-context.md")))
                        },
                    )

                    val detail = state.evidenceDetails.last()
                    assertSoftly {
                        detail.decisionState shouldBe "Pending"
                        detail.decisionSource shouldBe "mock-adapter"
                        detail.decisionUnavailableReason shouldBe
                            "Read-only projection: operator decisions are visible, but action execution is not wired in this slice."
                        detail.criteriaResult shouldBe "Not done: 1 item(s) need operator attention."
                    }
                }

                then("an isolated event yields empty related items and the flattened surface stays public-safe") {
                    val state = MobileOperatorStateContract.fromEvents(
                        workEvents {
                            started()
                        },
                    )

                    val detail = state.evidenceDetails.single()
                    assertSoftly {
                        detail.relatedEvents shouldBe emptyList()
                        detail.evidenceReferences shouldBe emptyList()
                        detail.decisionState shouldBe null
                        detail.decisionUnavailableReason shouldBe "unavailable for latest replay event."
                        detail.criteriaResult shouldBe "Decision queue: no items need operator attention."
                        buildString {
                            state.timeline.forEach { entry ->
                                appendLine("${entry.occurredAt} ${entry.source} ${entry.type} ${entry.stateLabel} ${entry.summary}")
                            }
                            state.evidenceDetails.forEach { lineDetail ->
                                appendLine(
                                    "${lineDetail.source} ${lineDetail.timestamp} ${lineDetail.summary} " +
                                        "${lineDetail.provenance} ${lineDetail.decisionUnavailableReason} ${lineDetail.criteriaResult}",
                                )
                            }
                        }.shouldBePublicSafe()
                    }
                }
            }
        }

        given("stored events with unsafe runtime identifiers") {
            `when`("the mobile contract input is decoded") {
                then("message-like ids are rejected before mobile projection without echoing them") {
                    val rawIdentifier = "123456789" + "012345678"
                    val unsafeEventId = "event:message:$rawIdentifier:started"
                    val unsafeEvent =
                        """{"id":"$unsafeEventId","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter",""" +
                            "\"workItemId\":\"agent-task:42\",\"type\":\"work.started\"," +
                            "\"payload\":{\"title\":\"Run public hygiene check\"}}"

                    val error = shouldThrow<IllegalArgumentException> {
                        MobileOperatorStateContract.fromEvents(
                            listOf(
                                WorkEventJson.decode(unsafeEvent),
                            ),
                        )
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Work event id")
                        shouldContain("private runtime")
                        shouldNotContain(unsafeEventId)
                        shouldNotContain(rawIdentifier)
                    }
                }
            }
        }
    })
