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
        }

        given("stored event configuration") {
            `when`("an empty external property map is parsed") {
                then("it uses the default sample runtime") {
                    val config = AgentDeskRuntimeConfigParser.parse(emptyMap())

                    config shouldBe AgentDeskRuntimeConfig.defaults()
                }
            }

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

            `when`("a local filesystem store location is provided") {
                then("it accepts the location as an app-owned config boundary") {
                    val linuxPath = privateLinuxPath("agent-desk-events.ndjson")
                    val macPath = privateMacPath("agent-desk-events.ndjson")
                    val windowsPath = windowsUserPath("agent-desk-events.ndjson")

                    EventStoreLocation.parse(linuxPath).toString() shouldBe linuxPath
                    EventStoreLocation.parse(macPath).toString() shouldBe macPath
                    EventStoreLocation.parse(windowsPath).toString() shouldBe windowsPath
                }
            }

            `when`("stored event properties are parsed from external names") {
                then("it applies validated shared app config rules") {
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

            `when`("mode and source are parsed from external names") {
                then("they do not depend on Kotlin enum names") {
                    AgentDeskMode.parse("stored-events") shouldBe AgentDeskMode.StoredEvents
                    RuntimeEventSourceKind.parse("local-event-store") shouldBe RuntimeEventSourceKind.LocalEventStore
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
                    val raw = privateLinuxPath("private-token.ndjson")

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

            `when`("the value contains private public-safety markers") {
                then("shared validation rejects each marker without echoing the raw value") {
                    unsafeEventStoreLocations().forEach { unsafe ->
                        val error = shouldThrow<ConfigValidationException> {
                            EventStoreLocation.parse(unsafe)
                        }

                        assertSoftly(error.message.orEmpty()) {
                            shouldContain("eventStoreLocation")
                            shouldNotContain(unsafe)
                            shouldNotContain(rawIdentifier())
                        }
                    }
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

            `when`("parser input contains unsafe raw values") {
                then("validation fails without echoing them") {
                    val error = shouldThrow<ConfigValidationException> {
                        AgentDeskRuntimeConfigParser.parse(
                            mapOf(
                                "mode" to "stored-events",
                                "source" to "local-event-store",
                                "eventStoreLocation" to privateLinuxPath("private-token.ndjson"),
                            ),
                        )
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("eventStoreLocation")
                        shouldNotContain("/home/")
                        shouldNotContain("private-token")
                    }
                }
            }
        }
    })

private fun privateLinuxPath(fileName: String): String = "/home/" + "operator/$fileName"

private fun privateMacPath(fileName: String): String = "/Users/" + "operator/$fileName"

private fun windowsUserPath(fileName: String): String = listOf("C:", "Users", "operator", "AgentDesk", fileName).joinToString("\\")

private fun rawIdentifier(): String = "123456789" + "012345678"

private fun unsafeEventStoreLocations(): List<String> = listOf(
    rawIdentifier(),
    "channel:${rawIdentifier()}",
    "message:${rawIdentifier()}",
    "session:local-agent-events.ndjson",
    "thread:local-review-events.ndjson",
    "agent:main:events.ndjson",
    "[subagent events]",
    "<conversation events>",
    listOf("raw", "transcript events").joinToString(" "),
    listOf("bearer", "credential-marker").joinToString(" "),
    "auth_token=value",
    "github_pat_123",
    "ghp_123",
    "op://agent-desk/event-store",
    "password=value",
    "secret=value",
    "xoxb-token",
    privateLinuxPath("secret-events.ndjson"),
    privateMacPath("secret-events.ndjson"),
    windowsUserPath("secret-events.ndjson"),
    "file:" + privateLinuxPath("secret-events.ndjson"),
    listOf("Open", "Claw runtime context").joinToString(""),
)
