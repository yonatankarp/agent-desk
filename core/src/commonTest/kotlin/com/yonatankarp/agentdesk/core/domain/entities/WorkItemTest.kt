package com.yonatankarp.agentdesk.core.domain.entities

import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class WorkItemTest :
    FunSpec({
        test("title and summary normalize whitespace") {
            val title = WorkItemTitle.parse("  Review   build logs  ")
            val summary = WorkSummary.parse("  CI failed   on the core test task.  ")

            title.value shouldBe "Review build logs"
            summary.value shouldBe "CI failed on the core test task."
        }

        test("rejects blank title and summary") {
            shouldThrow<IllegalArgumentException> {
                WorkItemTitle.parse("   ")
            }
            shouldThrow<IllegalArgumentException> {
                WorkSummary.parse("   ")
            }
        }

        test("rejects multiline title and summary") {
            shouldThrow<IllegalArgumentException> {
                WorkItemTitle.parse("Review logs\nand retry")
            }
            shouldThrow<IllegalArgumentException> {
                WorkSummary.parse("Build failed\nSee private log path")
            }
        }

        test("applies valid transition") {
            val item = WorkItem(
                id = WorkItemId.parse("agent-task:42"),
                title = WorkItemTitle.parse("Run public hygiene check"),
                status = WorkStatus.Queued,
            )

            val started = item.transitionTo(WorkStatus.Running)

            started.status shouldBe WorkStatus.Running
            item.status shouldBe WorkStatus.Queued
        }

        test("rejects invalid transition") {
            val item = WorkItem(
                id = WorkItemId.parse("agent-task:42"),
                title = WorkItemTitle.parse("Run public hygiene check"),
                status = WorkStatus.Succeeded,
            )

            shouldThrow<IllegalArgumentException> {
                item.transitionTo(WorkStatus.Running)
            }
        }
    })
