package com.yonatankarp.agentdesk.core.domain.events

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class WorkProvenanceTest :
    BehaviorSpec({
        given("a provenance id") {
            `when`("it is parsed from mixed-case public-safe input") {
                then("it normalizes to the shared identifier grammar") {
                    ProvenanceId.parse(" Project.Agent-Desk_01 ").toString() shouldBe "project.agent-desk_01"
                }
            }

            `when`("it contains private runtime markers") {
                then("it is rejected without echoing the raw value") {
                    val unsafe = "session:local-agent"

                    val error = shouldThrow<IllegalArgumentException> {
                        ProvenanceId.parse(unsafe)
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Provenance id")
                        shouldNotContain(unsafe)
                    }
                }
            }
        }
    })
