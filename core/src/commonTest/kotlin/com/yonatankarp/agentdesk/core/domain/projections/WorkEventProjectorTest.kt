package com.yonatankarp.agentdesk.core.domain.projections

import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus
import com.yonatankarp.agentdesk.core.fixtures.CoreFixtures
import com.yonatankarp.agentdesk.testfixtures.eventTimestampAt
import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBeEmptyProjection
import com.yonatankarp.agentdesk.testfixtures.workEvents
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

                    projection.shouldBeEmptyProjection()
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

        given("stale work derivation") {
            `when`("running work has no newer event within the stale threshold") {
                then("it is marked as stale attention without changing lifecycle status") {
                    val projection =
                        WorkEventProjector.project(
                            workEvents {
                                started()
                                started(workItemId = "agent-task:43", at = eventTimestampAt(hour = 22, minute = 1))
                            },
                        )

                    projection.workItems.first { it.id.toString() == "agent-task:42" }.status shouldBe WorkStatus.Running
                    projection.staleAttention.single().workItemId.toString() shouldBe "agent-task:42"
                    projection.staleAttention.single().status shouldBe WorkStatus.Running
                    projection.staleAttention.single().lastEventAt shouldBe CoreFixtures.startedAt
                    projection.staleAttention.single().staleForMinutes shouldBe 61
                }
            }

            `when`("running work is newer than the stale threshold") {
                then("it is not marked stale") {
                    val projection =
                        WorkEventProjector.project(
                            workEvents {
                                started()
                                started(workItemId = "agent-task:43", at = eventTimestampAt(minute = 30))
                            },
                        )

                    projection.staleAttention shouldBe emptyList()
                }
            }

            `when`("a custom stale threshold is provided") {
                then("it uses that threshold instead of the default") {
                    val projection =
                        WorkEventProjector.project(
                            events =
                            workEvents {
                                started()
                                started(workItemId = "agent-task:43", at = eventTimestampAt(minute = 30))
                            },
                            staleThreshold = StaleWorkThreshold.parseMinutes(30),
                        )

                    projection.staleAttention.single().staleForMinutes shouldBe 30
                }
            }

            `when`("work is terminal even though its last event is old") {
                then("it is not marked stale") {
                    val projection =
                        WorkEventProjector.project(
                            workEvents {
                                started()
                                succeeded()
                                started(workItemId = "agent-task:43", at = eventTimestampAt(hour = 22, minute = 30))
                            },
                        )

                    projection.staleAttention shouldBe emptyList()
                }
            }

            `when`("there are no accepted events") {
                then("stale attention is empty") {
                    WorkEventProjector.project(emptyList()).staleAttention shouldBe emptyList()
                }
            }

            `when`("running work is exactly at the stale threshold") {
                then("it is marked stale at the boundary") {
                    val projection =
                        WorkEventProjector.project(
                            workEvents {
                                started()
                                started(workItemId = "agent-task:43", at = eventTimestampAt(hour = 22, minute = 0))
                            },
                        )

                    projection.staleAttention.single().workItemId.toString() shouldBe "agent-task:42"
                    projection.staleAttention.single().staleForMinutes shouldBe 60
                }
            }

            `when`("running work is one minute under the stale threshold") {
                then("it is not marked stale") {
                    val projection =
                        WorkEventProjector.project(
                            workEvents {
                                started()
                                started(workItemId = "agent-task:43", at = eventTimestampAt(minute = 59))
                            },
                        )

                    projection.staleAttention shouldBe emptyList()
                }
            }

            `when`("terminal work is older than the stale threshold") {
                then("it is not marked stale") {
                    val projection =
                        WorkEventProjector.project(
                            workEvents {
                                started()
                                succeeded()
                                started(workItemId = "agent-task:43", at = eventTimestampAt(hour = 23, minute = 30))
                            },
                        )

                    projection.workItems.first { it.id.toString() == "agent-task:42" }.status shouldBe WorkStatus.Succeeded
                    projection.staleAttention shouldBe emptyList()
                }
            }

            `when`("blocked work is older than the stale threshold") {
                then("it is not marked stale because attention states already require humans") {
                    val projection =
                        WorkEventProjector.project(
                            workEvents {
                                started()
                                blocked()
                                started(workItemId = "agent-task:43", at = eventTimestampAt(hour = 23, minute = 30))
                            },
                        )

                    projection.workItems.first { it.id.toString() == "agent-task:42" }.status shouldBe WorkStatus.Blocked
                    projection.staleAttention shouldBe emptyList()
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
