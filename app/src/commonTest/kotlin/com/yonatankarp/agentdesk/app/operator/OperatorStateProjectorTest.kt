package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.app.fixtures.operatorState
import com.yonatankarp.agentdesk.core.domain.projections.WorkEventProjector
import com.yonatankarp.agentdesk.testfixtures.eventTimestampAt
import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import com.yonatankarp.agentdesk.testfixtures.workEvents
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class OperatorStateProjectorTest :
    BehaviorSpec({
        given("sanitized work events") {
            `when`("they form a valid sequence") {
                then("they project into reusable operator state") {
                    val state = operatorState {
                        started()
                        blocked()
                    }

                    state.workItems.map { it.id.toString() }.shouldContainExactly("agent-task:42")
                    state.workItems.single().status.name shouldBe "Blocked"
                    state.events.map { it.id.toString() }.shouldContainExactly(
                        "event:agent-task:42:started",
                        "event:agent-task:42:blocked",
                    )
                }
            }

            `when`("a projection carries stale attention") {
                then("the shared factory preserves the stale field") {
                    val projection = WorkEventProjector.project(
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

                    val state = OperatorState.from(projection)

                    state.staleAttention.single().workItemId.toString() shouldBe "agent-task:42"
                }
            }

            `when`("projection ignores an invalid sequence") {
                then("it rejects the sequence with a public-safe app error") {
                    val error = shouldThrow<OperatorStateProjectionException> {
                        operatorState { blocked() }
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Invalid event sequence")
                        shouldContain("existing started work item")
                        shouldBePublicSafe()
                    }
                }
            }
        }
    })
