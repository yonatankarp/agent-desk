package com.yonatankarp.agentdesk.testfixtures

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class TimestampFixturesTest :
    BehaviorSpec({
        given("the canonical fixture day") {
            `when`("a timestamp is requested at a minute offset") {
                then("it renders the canonical RFC 3339 instant") {
                    eventTimestampAt(minute = 5).value shouldBe "2026-06-02T21:05:00Z"
                }
            }

            `when`("hour and second are overridden") {
                then("they render zero-padded") {
                    eventTimestampAt(hour = 22, minute = 0, second = 7).value shouldBe "2026-06-02T22:00:07Z"
                }
            }

            `when`("a component is out of range") {
                then("construction fails") {
                    shouldThrow<IllegalArgumentException> { eventTimestampAt(minute = 60) }
                    shouldThrow<IllegalArgumentException> { eventTimestampAt(hour = 24, minute = 0) }
                    shouldThrow<IllegalArgumentException> { eventTimestampAt(minute = 0, second = 60) }
                }
            }
        }
    })
