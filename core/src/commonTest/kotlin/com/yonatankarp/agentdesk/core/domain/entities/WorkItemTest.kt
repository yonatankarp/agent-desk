package com.yonatankarp.agentdesk.core.domain.entities

import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus
import com.yonatankarp.agentdesk.core.fixtures.CoreFixtures
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class WorkItemTest :
    BehaviorSpec({
        given("a queued work item") {
            `when`("it transitions to running") {
                then("it returns a running copy and leaves the original queued") {
                    val item = CoreFixtures.workItem(status = WorkStatus.Queued)

                    val started = item.transitionTo(WorkStatus.Running)

                    started.status shouldBe WorkStatus.Running
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
