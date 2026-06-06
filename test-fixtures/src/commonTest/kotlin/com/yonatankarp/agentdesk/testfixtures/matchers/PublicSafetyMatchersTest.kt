package com.yonatankarp.agentdesk.testfixtures.matchers

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain

class PublicSafetyMatchersTest :
    BehaviorSpec({
        given("the public-safety matcher") {
            `when`("text is public-safe") {
                then("it passes") {
                    "Run public hygiene check: work.started for agent-task:42".shouldBePublicSafe()
                }
            }

            `when`("text leaks a private path") {
                then("it fails and names the violation") {
                    val leak = "/home/" + "operator/notes.txt"

                    val failure = shouldThrow<AssertionError> { "stored at $leak".shouldBePublicSafe() }

                    failure.message.orEmpty() shouldContain "/home/"
                }
            }

            `when`("text leaks credential-shaped content") {
                then("it fails regardless of casing") {
                    shouldThrow<AssertionError> { "Bearer abc".shouldBePublicSafe() }
                    shouldThrow<AssertionError> { "GHP_abcdef".shouldBePublicSafe() }
                    shouldThrow<AssertionError> { ("op:" + "//vault/item").shouldBePublicSafe() }
                }
            }

            `when`("text contains a raw numeric identifier") {
                then("it fails") {
                    val rawIdentifier = "123456789" + "012345678"

                    shouldThrow<AssertionError> { "channel $rawIdentifier".shouldBePublicSafe() }
                }
            }
        }
    })
