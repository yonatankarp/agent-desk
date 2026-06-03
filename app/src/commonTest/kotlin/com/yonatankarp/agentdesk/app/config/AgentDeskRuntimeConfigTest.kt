package com.yonatankarp.agentdesk.app.config

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class AgentDeskRuntimeConfigTest :
    BehaviorSpec({
        given("default runtime configuration") {
            `when`("defaults are requested") {
                then("sample mode uses the mock source without a local path") {
                    val config = AgentDeskRuntimeConfig.defaults()

                    config.mode shouldBe AgentDeskMode.Sample
                    config.source shouldBe RuntimeEventSourceKind.Mock
                    config.eventStoreLocation shouldBe null
                }
            }

            `when`("external properties omit all values") {
                then("the shared parser returns the same defaults") {
                    val config = AgentDeskRuntimeConfigParser.parse(emptyMap())

                    config shouldBe AgentDeskRuntimeConfig.defaults()
                }
            }
        }

        given("stored event configuration") {
            `when`("a sanitized store location is provided") {
                then("it accepts the local event store source") {
                    val config = AgentDeskRuntimeConfig(
                        mode = AgentDeskMode.StoredEvents,
                        source = RuntimeEventSourceKind.LocalEventStore,
                        eventStoreLocation = EventStoreLocation.parse("agent-desk-events.ndjson"),
                    )

                    config.eventStoreLocation.toString() shouldBe "agent-desk-events.ndjson"
                }
            }

            `when`("mode and source are parsed from external names") {
                then("they do not depend on Kotlin enum names") {
                    AgentDeskMode.parse("stored-events") shouldBe AgentDeskMode.StoredEvents
                    RuntimeEventSourceKind.parse("local-event-store") shouldBe RuntimeEventSourceKind.LocalEventStore
                }
            }

            `when`("external properties describe the local store") {
                then("the shared parser returns validated stored event config") {
                    val config = AgentDeskRuntimeConfigParser.parse(
                        mapOf(
                            "mode" to "stored-events",
                            "source" to "local-event-store",
                            "eventStoreLocation" to "agent-desk-events.ndjson",
                        ),
                    )

                    config.mode shouldBe AgentDeskMode.StoredEvents
                    config.source shouldBe RuntimeEventSourceKind.LocalEventStore
                    config.eventStoreLocation.toString() shouldBe "agent-desk-events.ndjson"
                }
            }
        }

        given("invalid mode and source combinations") {
            `when`("stored event mode uses the mock source") {
                then("validation fails without private values") {
                    val error = shouldThrow<ConfigValidationException> {
                        AgentDeskRuntimeConfig(
                            mode = AgentDeskMode.StoredEvents,
                            source = RuntimeEventSourceKind.Mock,
                            eventStoreLocation = EventStoreLocation.parse("agent-desk-events.ndjson"),
                        )
                    }

                    error.message shouldBe "stored event mode requires the local event store source"
                }
            }

            `when`("sample mode includes a store location") {
                then("validation rejects the unused location without echoing it") {
                    val error = shouldThrow<ConfigValidationException> {
                        AgentDeskRuntimeConfig(
                            mode = AgentDeskMode.Sample,
                            source = RuntimeEventSourceKind.Mock,
                            eventStoreLocation = EventStoreLocation.parse("agent-desk-events.ndjson"),
                        )
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("sample mode")
                        shouldNotContain("agent-desk-events.ndjson")
                    }
                }
            }
        }

        given("unsafe event store locations") {
            `when`("the value contains private path and secret-looking material") {
                then("validation rejects it without echoing the raw value") {
                    val raw = "/home/operator/private-token.ndjson"

                    val error = shouldThrow<ConfigValidationException> {
                        EventStoreLocation.parse(raw)
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("eventStoreLocation")
                        shouldNotContain("/home/")
                        shouldNotContain("token")
                    }
                }
            }

            `when`("the value is multiline") {
                then("validation rejects it as non-public-safe") {
                    val error = shouldThrow<ConfigValidationException> {
                        EventStoreLocation.parse("events.ndjson\nextra")
                    }

                    error.message shouldContain "single public-safe value"
                }
            }
        }

        given("unknown external configuration values") {
            `when`("mode or source names are not recognized") {
                then("validation fails without echoing raw values") {
                    val modeError = shouldThrow<ConfigValidationException> {
                        AgentDeskMode.parse("private-mode")
                    }
                    val sourceError = shouldThrow<ConfigValidationException> {
                        RuntimeEventSourceKind.parse("private-source")
                    }

                    modeError.message shouldBe "mode must be sample or stored-events"
                    sourceError.message shouldBe "source must be mock or local-event-store"
                }
            }
        }
    })
