package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.app.fixtures.AppFixtures.workBlockedEvent
import com.yonatankarp.agentdesk.app.fixtures.AppFixtures.workItemId
import com.yonatankarp.agentdesk.app.fixtures.AppFixtures.workStartedEvent
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class OperatorStateProjectorTest :
    BehaviorSpec({
        given("work events") {
            `when`("they form a valid sequence") {
                then("they project into shared operator state") {
                    val state = OperatorStateProjector.project(
                        listOf(
                            workStartedEvent(),
                            workBlockedEvent(),
                        ),
                    )

                    state.workItems.shouldHaveSize(1)
                    state.workItems.single().id shouldBe workItemId
                    state.events.shouldHaveSize(2)
                }
            }

            `when`("they cannot be reduced from the first event") {
                then("projection fails with a public-safe app error") {
                    val error = shouldThrow<OperatorStateProjectionException> {
                        OperatorStateProjector.project(listOf(workBlockedEvent()))
                    }

                    error.message shouldContain "Invalid event sequence:"
                }
            }
        }
    })
