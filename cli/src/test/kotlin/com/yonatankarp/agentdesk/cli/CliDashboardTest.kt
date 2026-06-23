package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.app.runtime.RuntimeHostReachabilityDiagnostics
import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files

class CliDashboardTest :
    BehaviorSpec({
        given("the CLI with no arguments") {
            `when`("it is run") {
                then("sample mode is the default public-safe output") {
                    val result = runCli()

                    result.exitCode shouldBe 0
                    result.output shouldContain "Agent Desk"
                    result.output shouldContain "sample-agent"
                    result.output.shouldBePublicSafe()
                    result.error shouldBe ""
                }
            }
        }

        given("an event file with started and blocked records") {
            `when`("it is rendered through --events") {
                then("event file input renders projected operator state") {
                    val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
                    Files.writeString(eventFile, "$STARTED_EVENT\n$BLOCKED_EVENT\n")

                    val result = runCli("--events", eventFile.toString())

                    result.exitCode shouldBe 0
                    result.output shouldContain "- [Blocked] agent-task:42 Run public hygiene check"
                    result.output shouldContain "- agent-task:42 Run public hygiene check (Blocked)"
                    result.output shouldContain "work.blocked agent-task:42 from mock-adapter"
                    result.output.shouldBePublicSafe()
                    result.error shouldBe ""
                }
            }
        }

        given("a started event on stdin") {
            `when`("it is rendered through --stdin") {
                then("stdin input renders projected operator state") {
                    val result = runCli("--stdin", input = "$STARTED_EVENT\n")

                    result.exitCode shouldBe 0
                    result.output shouldContain "- [Running] agent-task:42 Run public hygiene check"
                    result.output shouldContain "Attention queue\n- none"
                }
            }
        }

        given("an empty config file") {
            `when`("it is rendered through --config") {
                then("empty config file renders default sample state") {
                    val configFile = Files.createTempFile("agent-desk-cli-config", ".properties")
                    Files.writeString(configFile, "")

                    val result = runCli("--config", configFile.toString())

                    result.exitCode shouldBe 0
                    result.output shouldContain "Agent Desk"
                    result.output shouldContain "sample-agent"
                    result.output.shouldBePublicSafe()
                    result.error shouldBe ""
                }
            }
        }

        given("a stored-events config pointing at an event store") {
            `when`("it is rendered through --config") {
                then("stored event config renders projected operator state") {
                    val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
                    val configFile = Files.createTempFile("agent-desk-cli-config", ".properties")
                    Files.writeString(eventFile, "$STARTED_EVENT\n$BLOCKED_EVENT\n")
                    Files.writeString(
                        configFile,
                        """
                        mode=stored-events
                        source=local-event-store
                        eventStoreLocation=$eventFile
                        """.trimIndent(),
                    )

                    val result = runCli("--config", configFile.toString())

                    result.exitCode shouldBe 0
                    result.output shouldContain "Health"
                    result.output shouldContain "- Status: Healthy"
                    result.output shouldContain "- Replayed 2 event(s) into operator state."
                    result.output shouldContain "- Last event: 2026-06-02T21:05:00.123Z."
                    result.output shouldContain "- [Blocked] agent-task:42 Run public hygiene check"
                    result.output shouldContain "- agent-task:42 Run public hygiene check (Blocked)"
                    result.output shouldContain "work.blocked agent-task:42 from mock-adapter"
                    result.output.shouldBePublicSafe()
                    result.error shouldBe ""
                }
            }
        }

        given("a dashboard host config") {
            `when`("the configured host is reachable") {
                then("dashboard health renders the public host alias without endpoint details") {
                    val hostConfig = Files.createTempFile("agent-desk-host", ".properties")
                    Files.writeString(
                        hostConfig,
                        """
                        hostAlias=host:primary
                        hostEndpoint=http://localhost:65535
                        """.trimIndent(),
                    )

                    val result = runCli(
                        "--sample",
                        "--host-config",
                        hostConfig.toString(),
                        hostReachabilityCheck = { RuntimeHostReachabilityDiagnostics.reachable(it.alias) },
                    )

                    result.exitCode shouldBe 0
                    result.output shouldContain "- Status: Healthy"
                    result.output shouldContain
                        "- Diagnostic: Host reachability: host=host:primary state=reachable."
                    result.output shouldNotContain "localhost"
                    result.output shouldNotContain "65535"
                    result.output.shouldBePublicSafe()
                }
            }

            `when`("the configured host is unavailable") {
                then("dashboard health distinguishes source connectivity from empty work") {
                    val hostConfig = Files.createTempFile("agent-desk-host", ".properties")
                    Files.writeString(
                        hostConfig,
                        """
                        hostAlias=host:primary
                        hostEndpoint=http://localhost:65535
                        """.trimIndent(),
                    )

                    val result = runCli("--host-config", hostConfig.toString())

                    result.exitCode shouldBe 0
                    result.output shouldContain "- Status: Source disconnected"
                    result.output shouldContain
                        "- Diagnostic: Host reachability: host=host:primary state=unreachable failure=network-unavailable."
                    result.output shouldNotContain "localhost"
                    result.output shouldNotContain "65535"
                    result.output.shouldBePublicSafe()
                }
            }

            `when`("the host config is missing required fields") {
                then("dashboard health renders not configured") {
                    val hostConfig = Files.createTempFile("agent-desk-host", ".properties")
                    Files.writeString(hostConfig, "")

                    val result = runCli("--sample", "--host-config", hostConfig.toString())

                    result.exitCode shouldBe 0
                    result.output shouldContain "- Status: Source disconnected"
                    result.output shouldContain
                        "- Diagnostic: Host reachability: host=not-configured state=not-configured failure=missing-configuration."
                    result.output.shouldBePublicSafe()
                }
            }
        }

        given("a stored-events config whose store ends in a torn record") {
            `when`("it is rendered through --config") {
                then("stored event config with torn trailing record renders prefix and warns on stderr") {
                    val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
                    val configFile = Files.createTempFile("agent-desk-cli-config", ".properties")
                    Files.writeString(eventFile, "$STARTED_EVENT\n$BLOCKED_EVENT\n{\"id\":\"event:agent-task:46:sta")
                    Files.writeString(
                        configFile,
                        """
                        mode=stored-events
                        source=local-event-store
                        eventStoreLocation=$eventFile
                        """.trimIndent(),
                    )

                    val result = runCli("--config", configFile.toString())

                    result.exitCode shouldBe 0
                    result.output shouldContain "- Status: Partial import"
                    result.output shouldContain "- Recovered 2 committed event(s) from a partial import."
                    result.output shouldContain "- Source: current replay input."
                    result.output shouldContain "- Last replay: not recorded."
                    result.output shouldContain "- Next safe action: repair the event store before importing more observations."
                    result.output shouldContain "- [Blocked] agent-task:42 Run public hygiene check"
                    result.error shouldContain "Torn trailing record at line 3"
                    result.output.shouldBePublicSafe()
                    result.error.shouldBePublicSafe()
                    result.error shouldNotContain eventFile.toString()
                }
            }

            `when`("a single work item is inspected through --config") {
                then("inspect with torn trailing store renders prefix and warns on stderr") {
                    val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
                    val configFile = Files.createTempFile("agent-desk-cli-config", ".properties")
                    Files.writeString(eventFile, "$STARTED_EVENT\n{\"id\":\"event:agent-task:46:sta")
                    Files.writeString(
                        configFile,
                        """
                        mode=stored-events
                        source=local-event-store
                        eventStoreLocation=$eventFile
                        """.trimIndent(),
                    )

                    val result = runCli("inspect", "agent-task:42", "--config", configFile.toString())

                    result.exitCode shouldBe 0
                    result.output shouldContain "Work item agent-task:42"
                    result.error shouldContain "Torn trailing record at line 2"
                    result.error.shouldBePublicSafe()
                    result.error shouldNotContain eventFile.toString()
                }
            }
        }

        given("a private-looking record on stdin") {
            `when`("it is rendered") {
                then("invalid input fails without echoing raw private-looking data") {
                    val result = runCli("--stdin", input = """{"secret":"${privateLinuxPath("private-token.txt")}"}""")

                    result.exitCode shouldBe 1
                    result.error shouldContain "Invalid event record at line 1."
                    result.error.shouldBePublicSafe()
                    result.output shouldBe ""
                }
            }
        }

        given("a channel-like work item id on stdin") {
            `when`("it is rendered") {
                then("stdin input rejects channel-like work item ids without echoing them") {
                    val rawIdentifier = "123456789" + "012345678"
                    val unsafeWorkItemId = "channel:$rawIdentifier"
                    val unsafeEvent =
                        """{"id":"event:agent-task:42:started","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter",""" +
                            "\"workItemId\":\"$unsafeWorkItemId\",\"type\":\"work.started\"," +
                            "\"payload\":{\"title\":\"Run public hygiene check\"}}"

                    val result = runCli("--stdin", input = unsafeEvent)

                    result.exitCode shouldBe 1
                    result.error shouldContain "Invalid event record at line 1."
                    result.error shouldNotContain unsafeWorkItemId
                    result.error shouldNotContain rawIdentifier
                    result.error.shouldBePublicSafe()
                    result.output shouldBe ""
                }
            }
        }

        given("duplicate event ids on stdin") {
            `when`("they are rendered") {
                then("duplicate event ids fail with a clear line-numbered error") {
                    val result = runCli("--stdin", input = "$STARTED_EVENT\n$STARTED_EVENT\n")

                    result.exitCode shouldBe 1
                    result.error shouldContain "Duplicate work event id at line 2."
                    result.error.shouldBePublicSafe()
                    result.output shouldBe ""
                }
            }
        }

        given("an unsupported event type on stdin") {
            `when`("it is rendered") {
                then("unsupported event types fail with a clear line-numbered error") {
                    val result = runCli("--stdin", input = UNSUPPORTED_EVENT)

                    result.exitCode shouldBe 1
                    result.error shouldContain "Unsupported event type at line 1."
                    result.output shouldBe ""
                }
            }
        }

        given("an out-of-order event on stdin") {
            `when`("it is rendered") {
                then("out-of-order events fail with a clear projection error") {
                    val result = runCli("--stdin", input = BLOCKED_EVENT)

                    result.exitCode shouldBe 1
                    result.error shouldContain "Invalid event sequence:"
                    result.error.shouldBePublicSafe()
                    result.output shouldBe ""
                }
            }
        }

        given("blank stdin input") {
            `when`("it is rendered") {
                then("empty stdin input fails with a clear error") {
                    val result = runCli("--stdin", input = " \n")

                    result.exitCode shouldBe 1
                    result.error shouldContain "No event input provided."
                    result.output shouldBe ""
                }
            }
        }

        given("an unreadable event file path") {
            `when`("the CLI is run") {
                then("invalid event file path fails without echoing the path") {
                    val result = runCli("--events", " ${privateLinuxPath("private-token.txt")}")

                    result.exitCode shouldBe 1
                    result.error shouldContain "Event input file could not be read."
                    result.error.shouldBePublicSafe()
                    result.output shouldBe ""
                }
            }
        }

        given("an unreadable config file path") {
            `when`("the CLI is run") {
                then("invalid config file path fails without echoing the path") {
                    val result = runCli("--config", " ${privateLinuxPath("private-token.properties")}")

                    result.exitCode shouldBe 1
                    result.error shouldContain "Runtime config file could not be read."
                    result.error.shouldBePublicSafe()
                    result.output shouldBe ""
                }
            }
        }

        given("a stored-events config missing the event store location") {
            `when`("the CLI is run") {
                then("stored event config missing event store location fails safely") {
                    val configFile = Files.createTempFile("agent-desk-cli-config", ".properties")
                    Files.writeString(
                        configFile,
                        """
                        mode=stored-events
                        source=local-event-store
                        """.trimIndent(),
                    )

                    val result = runCli("--config", configFile.toString())

                    result.exitCode shouldBe 1
                    result.error shouldContain "Invalid runtime config: stored event mode requires eventStoreLocation"
                    result.error.shouldBePublicSafe()
                    result.output shouldBe ""
                }
            }
        }

        given("a stored-events config pointing at a private-looking location") {
            `when`("the CLI is run") {
                then("invalid runtime config rejects without echoing raw values") {
                    val configFile = Files.createTempFile("agent-desk-cli-config", ".properties")
                    Files.writeString(
                        configFile,
                        """
                        mode=stored-events
                        source=local-event-store
                        eventStoreLocation=${privateLinuxPath("private-token.ndjson")}
                        """.trimIndent(),
                    )

                    val result = runCli("--config", configFile.toString())

                    result.exitCode shouldBe 1
                    result.error shouldContain "Invalid runtime config:"
                    result.error.shouldBePublicSafe()
                    result.output shouldBe ""
                }
            }
        }

        given("a stored-events config with an invalid event store path") {
            `when`("the CLI is run") {
                then("stored event config with invalid event store path fails safely") {
                    val configFile = Files.createTempFile("agent-desk-cli-config", ".properties")
                    Files.writeString(
                        configFile,
                        """
                        mode=stored-events
                        source=local-event-store
                        eventStoreLocation=events\u0000broken.ndjson
                        """.trimIndent(),
                    )

                    val result = runCli("--config", configFile.toString())

                    result.exitCode shouldBe 1
                    result.error shouldContain "Configured event store could not be read."
                    result.error shouldNotContain "broken.ndjson"
                    result.error.shouldBePublicSafe()
                    result.output shouldBe ""
                }
            }
        }

        given("a stored-events config whose store holds an unsafe event id") {
            `when`("the CLI is run") {
                then("stored event config unsafe event ids fail safely") {
                    val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
                    val configFile = Files.createTempFile("agent-desk-cli-config", ".properties")
                    val unsafeEventId = "event:private-token:started"
                    val unsafeEvent =
                        """{"id":"$unsafeEventId","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter",""" +
                            "\"workItemId\":\"agent-task:42\",\"type\":\"work.started\"," +
                            "\"payload\":{\"title\":\"Run public hygiene check\"," +
                            "\"summary\":\"Agent accepted the task and started local checks.\"}}"
                    Files.writeString(eventFile, "$unsafeEvent\n")
                    Files.writeString(
                        configFile,
                        """
                        mode=stored-events
                        source=local-event-store
                        eventStoreLocation=$eventFile
                        """.trimIndent(),
                    )

                    val result = runCli("--config", configFile.toString())

                    result.exitCode shouldBe 1
                    result.error shouldContain "Corrupt work event record at line 1 in configured event store"
                    result.error shouldNotContain unsafeEventId
                    result.error.shouldBePublicSafe()
                    result.output shouldBe ""
                }
            }
        }
    })
