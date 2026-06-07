package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files
import kotlin.io.path.readLines

class AgentDeskCliDashboardTest :
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
                    result.output shouldContain "- [Blocked] agent-task:42 Run public hygiene check"
                    result.output shouldContain "- agent-task:42 Run public hygiene check (Blocked)"
                    result.output shouldContain "work.blocked agent-task:42 from mock-adapter"
                    result.output.shouldBePublicSafe()
                    result.error shouldBe ""
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
    })
