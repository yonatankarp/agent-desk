package com.yonatankarp.agentdesk.testfixtures

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec

class TimestampFixturesTest :
    BehaviorSpec({
        given("the canonical fixture day") {
            `when`("a component is out of range") {
                then("construction fails") {
                    shouldThrow<IllegalArgumentException> { eventTimestampAt(minute = 60) }
                    shouldThrow<IllegalArgumentException> { eventTimestampAt(hour = 24, minute = 0) }
                    shouldThrow<IllegalArgumentException> { eventTimestampAt(minute = 0, second = 60) }
                }
            }
        }
    })
