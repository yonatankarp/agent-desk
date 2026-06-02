package com.yonatankarp.agentdesk.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class WorkStatusTest :
    FunSpec({
        test("queued work can start or be canceled") {
            WorkStatus.Queued.canTransitionTo(WorkStatus.Running).shouldBeTrue()
            WorkStatus.Queued.canTransitionTo(WorkStatus.Canceled).shouldBeTrue()

            WorkStatus.Queued.canTransitionTo(WorkStatus.Succeeded).shouldBeFalse()
            WorkStatus.Queued.canTransitionTo(WorkStatus.Blocked).shouldBeFalse()
        }

        test("running work can enter attention states or finish") {
            WorkStatus.Running.canTransitionTo(WorkStatus.Waiting).shouldBeTrue()
            WorkStatus.Running.canTransitionTo(WorkStatus.NeedsDecision).shouldBeTrue()
            WorkStatus.Running.canTransitionTo(WorkStatus.Blocked).shouldBeTrue()
            WorkStatus.Running.canTransitionTo(WorkStatus.Succeeded).shouldBeTrue()
            WorkStatus.Running.canTransitionTo(WorkStatus.Failed).shouldBeTrue()
            WorkStatus.Running.canTransitionTo(WorkStatus.Canceled).shouldBeTrue()
        }

        test("attention states can resume or move between attention states") {
            WorkStatus.Waiting.canTransitionTo(WorkStatus.Running).shouldBeTrue()
            WorkStatus.NeedsDecision.canTransitionTo(WorkStatus.Blocked).shouldBeTrue()
            WorkStatus.Blocked.canTransitionTo(WorkStatus.NeedsDecision).shouldBeTrue()
        }

        test("terminal statuses cannot reopen") {
            WorkStatus.Succeeded.canTransitionTo(WorkStatus.Running).shouldBeFalse()
            WorkStatus.Failed.canTransitionTo(WorkStatus.Running).shouldBeFalse()
            WorkStatus.Canceled.canTransitionTo(WorkStatus.Running).shouldBeFalse()
        }

        test("human attention is explicit") {
            WorkStatus.NeedsDecision.requiresHumanAttention.shouldBeTrue()
            WorkStatus.Blocked.requiresHumanAttention.shouldBeTrue()

            WorkStatus.Waiting.requiresHumanAttention.shouldBeFalse()
            WorkStatus.Running.requiresHumanAttention.shouldBeFalse()
        }
    })
