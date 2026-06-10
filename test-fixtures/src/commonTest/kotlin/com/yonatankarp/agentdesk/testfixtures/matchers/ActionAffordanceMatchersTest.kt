package com.yonatankarp.agentdesk.testfixtures.matchers

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain

class ActionAffordanceMatchersTest :
    BehaviorSpec({
        given("the action-affordance matcher") {
            `when`("text only contains completion or audit labels that embed a verb") {
                then("it passes because matching is on word boundaries, not substrings") {
                    "[Canceled] agent-task:50 (Canceled outcome)".shouldHaveNoActionAffordances()
                    "Decision recorded: Approved".shouldHaveNoActionAffordances()
                }
            }

            `when`("text exposes a real action affordance") {
                then("it fails and names the verb") {
                    val failure =
                        shouldThrow<AssertionError> {
                            "[Running] agent-task:51 Cancel the deployment".shouldHaveNoActionAffordances()
                        }

                    failure.message.orEmpty() shouldContain "Cancel"
                }
            }

            `when`("text exposes a different action verb as a whole word") {
                then("it fails for that verb too") {
                    shouldThrow<AssertionError> { "Approve this proposal".shouldHaveNoActionAffordances() }
                }
            }
        }
    })
