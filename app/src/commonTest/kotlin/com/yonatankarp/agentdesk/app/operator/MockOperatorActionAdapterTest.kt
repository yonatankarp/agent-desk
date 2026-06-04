package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.app.fixtures.AppFixtures.workBlockedEvent
import com.yonatankarp.agentdesk.app.fixtures.AppFixtures.workItemId
import com.yonatankarp.agentdesk.app.fixtures.AppFixtures.workStartedEvent
import com.yonatankarp.agentdesk.app.fixtures.AppFixtures.workSucceededEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class MockOperatorActionAdapterTest :
    BehaviorSpec({
        given("a mock operator action adapter") {
            val adapter = MockOperatorActionAdapter()

            `when`("resume is requested for a blocked work item") {
                then("it emits a sanitized resume event") {
                    val event = adapter.perform(
                        intent = OperatorActionIntent.Resume,
                        workItemId = workItemId,
                        events = listOf(workStartedEvent(), workBlockedEvent()),
                    )

                    event.id.toString() shouldBe "event:agent-task:42:action-resume"
                    event.source.toString() shouldBe "mock-action-adapter"
                    event.workItemId shouldBe workItemId
                    (event.payload as WorkStartedPayload).summary.toString() shouldBe "Mock operator requested resume."
                    event.evidenceReferences.map { it.target.toString() }.shouldContainExactly("mock-action:resume")
                }
            }

            `when`("an unsupported action is requested") {
                then("it rejects without exposing private context") {
                    val error = shouldThrow<OperatorActionException> {
                        adapter.perform(
                            intent = OperatorActionIntent.Stop,
                            workItemId = workItemId,
                            events = listOf(workStartedEvent(), workBlockedEvent()),
                        )
                    }

                    assertPublicSafe(error.message.orEmpty())
                    error.message shouldContain "supports only resume"
                }
            }

            `when`("the work item is missing") {
                then("it rejects with a public-safe error") {
                    val error = shouldThrow<OperatorActionException> {
                        adapter.perform(
                            intent = OperatorActionIntent.Resume,
                            workItemId = WorkItemId.parse("agent-task:99"),
                            events = listOf(workStartedEvent(), workBlockedEvent()),
                        )
                    }

                    assertPublicSafe(error.message.orEmpty())
                    error.message shouldBe "Work item was not found."
                }
            }

            `when`("the work item is terminal") {
                then("it rejects resume") {
                    val error = shouldThrow<OperatorActionException> {
                        adapter.perform(
                            intent = OperatorActionIntent.Resume,
                            workItemId = workItemId,
                            events = listOf(workStartedEvent(), workSucceededEvent()),
                        )
                    }

                    assertPublicSafe(error.message.orEmpty())
                    error.message shouldContain "cannot be resumed"
                }
            }
        }
    })

private fun assertPublicSafe(text: String) {
    assertSoftly(text) {
        shouldNotContain("/home/")
        shouldNotContain("private-token")
        shouldNotContain("discord")
        shouldNotContain("op://")
    }
}
