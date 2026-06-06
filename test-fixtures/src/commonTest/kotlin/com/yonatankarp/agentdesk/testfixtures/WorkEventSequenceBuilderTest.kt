package com.yonatankarp.agentdesk.testfixtures

import com.yonatankarp.agentdesk.core.domain.events.WorkBlockedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkEventType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class WorkEventSequenceBuilderTest :
    BehaviorSpec({
        given("the workEvents builder") {
            `when`("a canonical started/blocked chain is built") {
                then("it matches the canonical fixtures") {
                    val events = workEvents {
                        started()
                        blocked()
                    }

                    events shouldBe listOf(
                        WorkEventFixtures.workStartedEvent(),
                        WorkEventFixtures.workBlockedEvent(),
                    )
                }
            }

            `when`("a second work item is described with raw strings") {
                then("ids and payload values are derived and parsed") {
                    val events = workEvents {
                        started(workItemId = "agent-task:43", at = eventTimestampAt(minute = 1))
                        succeeded(workItemId = "agent-task:43", at = eventTimestampAt(minute = 2))
                    }

                    events.map { it.id.value } shouldContainExactly listOf(
                        "event:agent-task:43:started",
                        "event:agent-task:43:succeeded",
                    )
                    events.map { it.workItemId.value }.toSet() shouldBe setOf("agent-task:43")
                }
            }

            `when`("a blocked reason and evidence are provided") {
                then("they land on the event") {
                    val events = workEvents {
                        blocked(
                            reason = "CI failed twice in a row.",
                            evidence = listOf(checkRunEvidence("CI run", "https://github.com/x/y/actions/runs/1")),
                        )
                    }

                    val payload = events.single().payload.shouldBeInstanceOf<WorkBlockedPayload>()
                    payload.reason.value shouldBe "CI failed twice in a row."
                    events.single().evidenceReferences.single().label.value shouldBe "CI run"
                }
            }

            `when`("a pre-built event is appended") {
                then("it is kept verbatim and in order") {
                    val custom = WorkEventFixtures.workNeedsDecisionEvent()

                    val events = workEvents {
                        started()
                        event(custom)
                    }

                    events.map { it.type } shouldContainExactly listOf(
                        WorkEventType.WorkStarted,
                        WorkEventType.WorkNeedsDecision,
                    )
                    events[1] shouldBe custom
                }
            }

            `when`("decision and terminal events are built with defaults") {
                then("they match the canonical fixtures") {
                    val events = workEvents {
                        needsDecision()
                        failed()
                        canceled()
                    }

                    events shouldBe listOf(
                        WorkEventFixtures.workNeedsDecisionEvent(),
                        WorkEventFixtures.workFailedEvent(),
                        WorkEventFixtures.workCanceledEvent(),
                    )
                }
            }
        }
    })
