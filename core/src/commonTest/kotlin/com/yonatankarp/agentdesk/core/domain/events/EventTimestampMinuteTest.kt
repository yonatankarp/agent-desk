package com.yonatankarp.agentdesk.core.domain.events

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.longs.shouldBeExactly
import io.kotest.matchers.string.shouldContain

class EventTimestampMinuteTest :
    BehaviorSpec({
        given("an RFC 3339 UTC timestamp") {
            `when`("it is at the Unix epoch") {
                then("its epoch minute is zero") {
                    EventTimestamp.parse("1970-01-01T00:00:00Z").epochMinute() shouldBeExactly 0
                }
            }

            `when`("it includes fractional seconds") {
                then("fractional seconds do not change minute precision") {
                    val wholeSecond = EventTimestamp.parse("2026-06-02T21:05:00Z")
                    val fractionalSecond = EventTimestamp.parse("2026-06-02T21:05:00.123Z")

                    fractionalSecond.epochMinute() shouldBeExactly wholeSecond.epochMinute()
                }
            }

            `when`("it crosses a year boundary") {
                then("adjacent minutes stay adjacent") {
                    val previous = EventTimestamp.parse("2025-12-31T23:59:00Z")
                    val next = EventTimestamp.parse("2026-01-01T00:00:00Z")

                    next.epochMinute() - previous.epochMinute() shouldBeExactly 1
                }
            }

            `when`("it crosses a month boundary") {
                then("adjacent minutes stay adjacent") {
                    val previous = EventTimestamp.parse("2026-02-28T23:59:00Z")
                    val next = EventTimestamp.parse("2026-03-01T00:00:00Z")

                    next.epochMinute() - previous.epochMinute() shouldBeExactly 1
                }
            }

            `when`("it crosses a leap day boundary") {
                then("adjacent minutes stay adjacent") {
                    val previous = EventTimestamp.parse("2024-02-29T23:59:00Z")
                    val next = EventTimestamp.parse("2024-03-01T00:00:00Z")

                    next.epochMinute() - previous.epochMinute() shouldBeExactly 1
                }
            }
        }

        given("a non-UTC timestamp") {
            `when`("it is parsed") {
                then("the existing validation error is preserved") {
                    val error = shouldThrow<IllegalArgumentException> {
                        EventTimestamp.parse("2026-06-02T21:00:00+02:00")
                    }

                    error.message shouldContain "Event timestamp must be an RFC 3339 UTC instant"
                }
            }
        }
    })
