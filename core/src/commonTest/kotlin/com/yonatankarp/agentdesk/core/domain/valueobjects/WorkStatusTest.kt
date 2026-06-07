package com.yonatankarp.agentdesk.core.domain.valueobjects

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class WorkStatusTest :
    BehaviorSpec({
        given("queued work") {
            `when`("it transitions") {
                then("it can start or be canceled") {
                    WorkStatus.Queued.canTransitionTo(WorkStatus.Running).shouldBeTrue()
                    WorkStatus.Queued.canTransitionTo(WorkStatus.Canceled).shouldBeTrue()

                    WorkStatus.Queued.canTransitionTo(WorkStatus.Succeeded).shouldBeFalse()
                    WorkStatus.Queued.canTransitionTo(WorkStatus.Blocked).shouldBeFalse()
                }
            }
        }

        given("running work") {
            `when`("it transitions") {
                then("it can enter attention states or finish") {
                    WorkStatus.Running.canTransitionTo(WorkStatus.Waiting).shouldBeTrue()
                    WorkStatus.Running.canTransitionTo(WorkStatus.NeedsDecision).shouldBeTrue()
                    WorkStatus.Running.canTransitionTo(WorkStatus.Blocked).shouldBeTrue()
                    WorkStatus.Running.canTransitionTo(WorkStatus.Succeeded).shouldBeTrue()
                    WorkStatus.Running.canTransitionTo(WorkStatus.Failed).shouldBeTrue()
                    WorkStatus.Running.canTransitionTo(WorkStatus.Canceled).shouldBeTrue()
                }
            }
        }

        given("attention states") {
            `when`("they transition") {
                then("they can resume or move between attention states") {
                    WorkStatus.Waiting.canTransitionTo(WorkStatus.Running).shouldBeTrue()
                    WorkStatus.NeedsDecision.canTransitionTo(WorkStatus.Blocked).shouldBeTrue()
                    WorkStatus.Blocked.canTransitionTo(WorkStatus.NeedsDecision).shouldBeTrue()
                }
            }
        }

        given("terminal statuses") {
            `when`("they transition") {
                then("they cannot reopen") {
                    WorkStatus.Succeeded.canTransitionTo(WorkStatus.Running).shouldBeFalse()
                    WorkStatus.Failed.canTransitionTo(WorkStatus.Running).shouldBeFalse()
                    WorkStatus.Canceled.canTransitionTo(WorkStatus.Running).shouldBeFalse()
                }
            }
        }

        given("human attention states") {
            `when`("attention is inspected") {
                then("only explicit attention states require human attention") {
                    WorkStatus.NeedsDecision.requiresHumanAttention.shouldBeTrue()
                    WorkStatus.Blocked.requiresHumanAttention.shouldBeTrue()

                    WorkStatus.Waiting.requiresHumanAttention.shouldBeFalse()
                    WorkStatus.Running.requiresHumanAttention.shouldBeFalse()
                }
            }
        }

        given("resumable states") {
            `when`("resumability is inspected") {
                then("only waiting and attention states are resumable") {
                    WorkStatus.Waiting.isResumable.shouldBeTrue()
                    WorkStatus.NeedsDecision.isResumable.shouldBeTrue()
                    WorkStatus.Blocked.isResumable.shouldBeTrue()

                    WorkStatus.Queued.isResumable.shouldBeFalse()
                    WorkStatus.Running.isResumable.shouldBeFalse()
                    WorkStatus.Succeeded.isResumable.shouldBeFalse()
                    WorkStatus.Failed.isResumable.shouldBeFalse()
                    WorkStatus.Canceled.isResumable.shouldBeFalse()
                }
            }
        }

        given("self transitions") {
            `when`("a status transitions to itself") {
                then("it is allowed for every status including terminal ones") {
                    WorkStatus.entries.forEach { status ->
                        status.canTransitionTo(status).shouldBeTrue()
                    }
                }
            }
        }

        given("cross-terminal transitions") {
            `when`("one terminal status moves to another") {
                then("it is rejected") {
                    WorkStatus.Succeeded.canTransitionTo(WorkStatus.Failed).shouldBeFalse()
                    WorkStatus.Succeeded.canTransitionTo(WorkStatus.Canceled).shouldBeFalse()
                    WorkStatus.Failed.canTransitionTo(WorkStatus.Succeeded).shouldBeFalse()
                    WorkStatus.Canceled.canTransitionTo(WorkStatus.Succeeded).shouldBeFalse()
                }
            }
        }
    })
