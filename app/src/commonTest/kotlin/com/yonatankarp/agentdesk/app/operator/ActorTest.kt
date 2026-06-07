package com.yonatankarp.agentdesk.app.operator

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

class ActorTest :
    BehaviorSpec({
        given("an actor") {
            `when`("raw input has surrounding whitespace and mixed case") {
                then("it normalizes whitespace and case") {
                    val actor = Actor.parse("  Operator:Daily-Agent  ")

                    actor.value shouldBe "operator:daily-agent"
                }
            }

            `when`("raw input is blank") {
                then("it rejects the value") {
                    shouldThrow<IllegalArgumentException> {
                        Actor.parse("   ")
                    }
                }
            }

            `when`("raw input has unsupported characters") {
                then("it rejects the value") {
                    shouldThrow<IllegalArgumentException> {
                        Actor.parse("operator alice")
                    }
                }
            }

            `when`("raw input looks like a private runtime identifier") {
                then("it rejects the value without echoing it") {
                    val channelId = "channel:123456789012345678"

                    val error = shouldThrow<IllegalArgumentException> {
                        Actor.parse(channelId)
                    }

                    error.message.orEmpty() shouldNotContain channelId
                }
            }
        }
    })
