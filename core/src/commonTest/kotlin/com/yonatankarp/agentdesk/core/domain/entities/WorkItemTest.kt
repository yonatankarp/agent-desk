package com.yonatankarp.agentdesk.core.domain.entities

import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus
import com.yonatankarp.agentdesk.core.fixtures.CoreFixtures
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class WorkItemTest :
    FunSpec({
        test("applies valid transition") {
            val item = CoreFixtures.workItem(status = WorkStatus.Queued)

            val started = item.transitionTo(WorkStatus.Running)

            started.status shouldBe WorkStatus.Running
            item.status shouldBe WorkStatus.Queued
        }

        test("rejects invalid transition") {
            val item = CoreFixtures.workItem(status = WorkStatus.Succeeded)

            shouldThrow<IllegalArgumentException> {
                item.transitionTo(WorkStatus.Running)
            }
        }
    })
