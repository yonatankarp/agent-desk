package com.yonatankarp.agentdesk.app.operator.decision

import com.yonatankarp.agentdesk.app.fixtures.operatorState
import com.yonatankarp.agentdesk.core.domain.events.EventSource
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.testfixtures.sanitizedNoteEvidence
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class DecisionQueueProjectorTest :
    BehaviorSpec({
        given("operator state with decision requests") {
            `when`("a needs-decision event has public-safe evidence") {
                then("it creates a pending read-only decision queue item") {
                    val state = operatorState {
                        started()
                        needsDecision(evidence = listOf(sanitizedNoteEvidence("Decision context", "docs/decision-context.md")))
                    }

                    val projection = DecisionQueueProjector.project(state)
                    val item = projection.items.single()

                    assertSoftly {
                        projection.states.shouldContainExactly(DecisionQueueItemState.Pending)
                        item.id shouldBe "decision:event:agent-task:42:needs-decision"
                        item.workItemId shouldBe WorkItemId.parse("agent-task:42")
                        item.request.prompt shouldBe "Operator decision needed."
                        item.request.source shouldBe EventSource.parse("mock-adapter")
                        item.request.risk shouldBe DecisionRisk.ReadOnly
                        item.request.urgency shouldBe DecisionUrgency.Normal
                        item.request.recommendedOptionId shouldBe "inspect-evidence"
                        item.request.options.map { it.id }.shouldContainExactly(
                            "inspect-evidence",
                            "defer",
                            "mark-insufficient-evidence",
                        )
                        item.request.evidenceReferences.single().target shouldBe "docs/decision-context.md"
                        item.unavailableReason shouldContain "Read-only projection"
                        item.unavailableReason shouldContain "action execution is not wired"
                    }
                }
            }

            `when`("a needs-decision event has no evidence") {
                then("it marks the queue item as insufficient evidence") {
                    val state = operatorState {
                        started()
                        needsDecision()
                    }

                    val item = DecisionQueueProjector.project(state).items.single()

                    item.state shouldBe DecisionQueueItemState.InsufficientEvidence
                    item.request.recommendedOptionId shouldBe "mark-insufficient-evidence"
                    item.unavailableReason shouldContain "more public-safe evidence is required"
                }
            }

            `when`("later replay state resolves the work") {
                then("it keeps the historical request but marks it superseded") {
                    val state = operatorState {
                        started()
                        needsDecision()
                        failed()
                    }

                    val item = DecisionQueueProjector.project(state).items.single()

                    item.state shouldBe DecisionQueueItemState.Superseded
                    item.updatedAt shouldBe EventTimestamp.parse("2026-06-02T21:10:00Z")
                    item.unavailableReason shouldContain "later replay state superseded"
                }
            }
        }

        given("decision request validation") {
            `when`("request text is not public-safe") {
                then("it rejects without echoing the unsafe value") {
                    val unsafePrompt = "Read " + "/" + "home/user/private.log"

                    val error = shouldThrow<IllegalArgumentException> {
                        DecisionRequest(
                            prompt = unsafePrompt,
                            options = listOf(
                                DecisionOption(
                                    id = "inspect-evidence",
                                    label = "Inspect evidence",
                                    consequence = "Operator reviews sanitized evidence.",
                                ),
                            ),
                            recommendedOptionId = "inspect-evidence",
                            consequences = "No action is executed.",
                            risk = DecisionRisk.ReadOnly,
                            evidenceReferences = emptyList(),
                            source = EventSource.parse("mock-adapter"),
                            expiresAt = null,
                            urgency = DecisionUrgency.Normal,
                        )
                    }

                    error.message.orEmpty() shouldContain "Decision prompt"
                    error.message.orEmpty() shouldNotContain unsafePrompt
                }
            }
        }
    })
