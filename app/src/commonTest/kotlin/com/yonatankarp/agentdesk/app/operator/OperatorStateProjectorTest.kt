package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.app.fixtures.AppFixtures.workBlockedEvent
import com.yonatankarp.agentdesk.app.fixtures.AppFixtures.workStartedEvent
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class OperatorStateProjectorTest :
    BehaviorSpec({
        given("sanitized work events") {
            `when`("they form a valid sequence") {
                then("they project into reusable operator state") {
                    val state = OperatorStateProjector.project(
                        listOf(
                            workStartedEvent(),
                            workBlockedEvent(),
                        ),
                    )

                    state.workItems.map { it.id.toString() }.shouldContainExactly("agent-task:42")
                    state.workItems.single().status.name shouldBe "Blocked"
                    state.events.map { it.id.toString() }.shouldContainExactly(
                        "event:agent-task:42:started",
                        "event:agent-task:42:blocked",
                    )
                }
            }

            `when`("projection ignores an invalid sequence") {
                then("it rejects the sequence with a public-safe app error") {
                    val error = shouldThrow<OperatorStateProjectionException> {
                        OperatorStateProjector.project(listOf(workBlockedEvent()))
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Invalid event sequence")
                        shouldContain("existing started work item")
                        shouldNotContain("/home/")
                        shouldNotContain("private-token")
                        shouldNotContain("discord")
                        shouldNotContain("op://")
                    }
                }
            }
        }
    })
