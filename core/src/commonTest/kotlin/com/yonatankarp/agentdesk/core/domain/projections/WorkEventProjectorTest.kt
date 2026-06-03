package com.yonatankarp.agentdesk.core.domain.projections

import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus
import com.yonatankarp.agentdesk.core.fixtures.CoreFixtures
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class WorkEventProjectorTest :
    BehaviorSpec({
        given("an empty event stream") {
            `when`("it is projected") {
                then("it returns empty current state") {
                    val projection = WorkEventProjector.project(emptyList())

                    projection.workItems shouldBe emptyList()
                    projection.recentEvents shouldBe emptyList()
                    projection.ignoredEvents shouldBe emptyList()
                }
            }
        }

        given("a started event") {
            `when`("it is projected") {
                then("it creates a running work item") {
                    val projection = WorkEventProjector.project(listOf(CoreFixtures.workStartedEvent()))

                    projection.workItems.single().status shouldBe WorkStatus.Running
                    projection.workItems.single().title shouldBe CoreFixtures.workTitle
                    projection.workItems.single().summary shouldBe CoreFixtures.startedSummary
                    projection.recentEvents.map { it.id.toString() }.shouldContainExactly("event:agent-task:42:started")
                }
            }
        }

        given("attention events") {
            `when`("started work needs a decision and then becomes blocked") {
                then("the latest lifecycle state requires human attention") {
                    val projection =
                        WorkEventProjector.project(
                            listOf(
                                CoreFixtures.workStartedEvent(),
                                CoreFixtures.workNeedsDecisionEvent(),
                                CoreFixtures.workBlockedEvent(),
                            ),
                        )

                    projection.workItems.single().status shouldBe WorkStatus.Blocked
                    projection.workItems.single().status.requiresHumanAttention shouldBe true
                    projection.workItems.single().summary shouldBe CoreFixtures.blockedReason
                }
            }
        }

        given("terminal events") {
            `when`("running work succeeds") {
                then("the projected item is terminal") {
                    val projection =
                        WorkEventProjector.project(
                            listOf(
                                CoreFixtures.workStartedEvent(),
                                CoreFixtures.workSucceededEvent(),
                            ),
                        )

                    projection.workItems.single().status shouldBe WorkStatus.Succeeded
                    projection.workItems.single().status.isTerminal shouldBe true
                }
            }
        }

        given("duplicate event ids") {
            `when`("a duplicate id appears later in the stream") {
                then("the duplicate is ignored deterministically") {
                    val duplicateId = WorkEventId.parse("event:agent-task:42:duplicate")
                    val projection =
                        WorkEventProjector.project(
                            listOf(
                                CoreFixtures.workStartedEvent(id = duplicateId),
                                CoreFixtures.workBlockedEvent(id = duplicateId),
                            ),
                        )

                    projection.workItems.single().status shouldBe WorkStatus.Running
                    projection.recentEvents.map { it.id }.shouldContainExactly(duplicateId)
                    projection.ignoredEvents.single().reason shouldContain "Duplicate"
                }
            }
        }

        given("out-of-order or invalid transitions") {
            `when`("a blocked event appears before work has started") {
                then("the event is ignored instead of inventing missing work") {
                    val projection =
                        WorkEventProjector.project(
                            listOf(
                                CoreFixtures.workBlockedEvent(),
                                CoreFixtures.workStartedEvent(),
                            ),
                        )

                    projection.workItems.single().status shouldBe WorkStatus.Running
                    projection.recentEvents.map { it.type.wireName }.shouldContainExactly("work.started")
                    projection.ignoredEvents.single().reason shouldContain "requires an existing started work item"
                }
            }

            `when`("an event tries to leave a terminal state") {
                then("the invalid transition is ignored") {
                    val projection =
                        WorkEventProjector.project(
                            listOf(
                                CoreFixtures.workStartedEvent(),
                                CoreFixtures.workSucceededEvent(),
                                CoreFixtures.workBlockedEvent(id = WorkEventId.parse("event:agent-task:42:blocked-late")),
                            ),
                        )

                    projection.workItems.single().status shouldBe WorkStatus.Succeeded
                    projection.ignoredEvents.single().reason shouldContain "Cannot transition"
                }
            }
        }
    })
