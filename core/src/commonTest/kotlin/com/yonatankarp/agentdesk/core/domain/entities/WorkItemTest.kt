package com.yonatankarp.agentdesk.core.domain.entities

import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary
import com.yonatankarp.agentdesk.core.fixtures.CoreFixtures
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class WorkItemTest :
    BehaviorSpec({
        given("a queued work item with a summary") {
            `when`("it transitions to running") {
                then("only the status changes; identity, title, and summary are preserved") {
                    val summary = WorkSummary.parse("Waiting for the build runner.")
                    val item = CoreFixtures.workItem(status = WorkStatus.Queued, summary = summary)

                    val started = item.transitionTo(WorkStatus.Running)

                    assertSoftly(started) {
                        status shouldBe WorkStatus.Running
                        id shouldBe item.id
                        title shouldBe item.title
                        this.summary shouldBe summary
                    }
                    item.status shouldBe WorkStatus.Queued
                }
            }
        }

        given("a terminal work item") {
            `when`("it transitions back to running") {
                then("it rejects the invalid transition") {
                    val item = CoreFixtures.workItem(status = WorkStatus.Succeeded)

                    shouldThrow<IllegalArgumentException> {
                        item.transitionTo(WorkStatus.Running)
                    }
                }
            }
        }
    })
