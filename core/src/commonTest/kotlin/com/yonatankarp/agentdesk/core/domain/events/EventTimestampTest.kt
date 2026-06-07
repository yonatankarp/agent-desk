package com.yonatankarp.agentdesk.core.domain.events

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe

class EventTimestampTest :
    BehaviorSpec({
        given("event timestamps with differing fractional-second precision") {
            `when`("a fractional instant is compared against the same whole second") {
                then("the fractional instant orders after the whole second") {
                    val wholeSecond = EventTimestamp.parse("2026-06-02T21:00:00Z")
                    val fractional = EventTimestamp.parse("2026-06-02T21:00:00.5Z")

                    fractional shouldBeGreaterThan wholeSecond
                    wholeSecond shouldBeLessThan fractional
                }
            }

            `when`("the same instant is written at different precision") {
                then("the instants compare equal") {
                    val short = EventTimestamp.parse("2026-06-02T21:00:00.5Z")
                    val long = EventTimestamp.parse("2026-06-02T21:00:00.500Z")

                    short.compareTo(long) shouldBe 0
                    short shouldBe long
                }
            }

            `when`("a maximal fraction is compared against the next whole second") {
                then("the next whole second orders after the fraction") {
                    val fraction = EventTimestamp.parse("2026-06-02T21:00:00.999999999Z")
                    val nextSecond = EventTimestamp.parse("2026-06-02T21:00:01Z")

                    nextSecond shouldBeGreaterThan fraction
                }
            }

            `when`("a fraction consists only of trailing zeros") {
                then("the canonical value drops the empty fraction") {
                    val timestamp = EventTimestamp.parse("2026-06-02T21:00:00.000Z")

                    timestamp.value shouldBe "2026-06-02T21:00:00Z"
                    timestamp shouldBe EventTimestamp.parse("2026-06-02T21:00:00Z")
                }
            }

            `when`("a fraction carries trailing zeros") {
                then("the canonical value trims them") {
                    val timestamp = EventTimestamp.parse("2026-06-02T21:00:00.250Z")

                    timestamp.value shouldBe "2026-06-02T21:00:00.25Z"
                }
            }
        }

        given("an invalid event timestamp") {
            `when`("the input is not an RFC 3339 UTC instant") {
                then("it rejects the value") {
                    shouldThrow<IllegalArgumentException> {
                        EventTimestamp.parse("2026-06-02 21:00:00")
                    }
                }
            }
        }
    })
