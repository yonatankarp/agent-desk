package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.app.fixtures.AppFixtures
import com.yonatankarp.agentdesk.testfixtures.workEvents
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class WorkItemInspectorTest :
    BehaviorSpec({
        given("accepted work events") {
            `when`("a work item is inspected") {
                then("it selects the item and filters accepted event lines") {
                    val inspection = WorkItemInspector.inspect(
                        events = workEvents {
                            started()
                            blocked()
                            started(workItemId = "agent-task:43")
                        },
                        workItemId = AppFixtures.workItemId,
                    )

                    val actual = inspection ?: error("Expected inspection")

                    actual.item.id shouldBe AppFixtures.workItemId
                    actual.statusPresentation.label shouldBe "Blocked"
                    actual.requiresAttention shouldBe true
                    actual.acceptedEvents.map { it.workItemId }.shouldContainExactly(
                        "agent-task:42",
                        "agent-task:42",
                    )
                    actual.projectionWarnings shouldBe emptyList()
                }
            }
        }

        given("projection warnings") {
            `when`("an ignored event belongs to the inspected item") {
                then("the warning is preserved without dropping accepted state") {
                    val inspection = WorkItemInspector.inspect(
                        events = workEvents {
                            started()
                            succeeded()
                            blocked()
                        },
                        workItemId = AppFixtures.workItemId,
                    )

                    val actual = inspection ?: error("Expected inspection")

                    actual.statusPresentation.label shouldBe "Succeeded"
                    actual.isTerminal shouldBe true
                    actual.projectionWarnings.shouldHaveSize(1)
                    actual.projectionWarnings.first().eventId shouldBe "event:agent-task:42:blocked"
                }
            }
        }
    })
