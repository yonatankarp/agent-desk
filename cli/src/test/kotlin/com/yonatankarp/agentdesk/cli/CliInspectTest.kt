package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files

class CliInspectTest :
    BehaviorSpec({
        given("the inspect command") {
            `when`("a sample work item is inspected") {
                then("inspect sample item renders item details") {
                    val result = runCli("inspect", "agent-task:43")

                    result.exitCode shouldBe 0
                    result.output shouldContain "Work item agent-task:43"
                    result.output shouldContain "Status: Needs decision"
                    result.output shouldContain "Title: Choose adapter boundary"
                    result.output shouldContain "Attention: yes"
                    result.output shouldContain "Accepted recent events"
                    result.output shouldContain "Projection warnings\n- none"
                    result.output shouldContain "Evidence references\n- none"
                    result.output.shouldBePublicSafe()
                    result.error shouldBe ""
                }
            }

            `when`("an event file with multiple work items is inspected") {
                then("inspect event file renders only selected work item events") {
                    val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
                    Files.writeString(eventFile, "$STARTED_EVENT\n$BLOCKED_EVENT\n$OTHER_STARTED_EVENT\n")

                    val result = runCli("inspect", "agent-task:42", "--events", eventFile.toString())

                    result.exitCode shouldBe 0
                    result.output shouldContain "Work item agent-task:42"
                    result.output shouldContain "Status: Blocked"
                    result.output shouldContain "Summary: CI failed on the core test task."
                    result.output shouldContain "work.started from mock-adapter"
                    result.output shouldContain "work.blocked from mock-adapter"
                    result.output shouldNotContain "Prepare release checklist"
                    result.output.shouldBePublicSafe()
                    result.error shouldBe ""
                }
            }

            `when`("a terminal item is inspected from stdin") {
                then("inspect stdin renders terminal item and projection warnings") {
                    val result = runCli(
                        "inspect",
                        "agent-task:42",
                        "--stdin",
                        input = "$STARTED_EVENT\n$SUCCEEDED_EVENT\n$BLOCKED_EVENT\n",
                    )

                    result.exitCode shouldBe 0
                    result.output shouldContain "Status: Succeeded"
                    result.output shouldContain "Terminal: yes"
                    result.output shouldContain "Projection warnings"
                    result.output shouldContain "ignored event - Cannot transition work item agent-task:42"
                    result.output.shouldBePublicSafe()
                    result.error shouldBe ""
                }
            }

            `when`("a stored-events config is used") {
                then("inspect stored event config uses configured input mode") {
                    val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
                    val configFile = Files.createTempFile("agent-desk-cli-config", ".properties")
                    Files.writeString(eventFile, "$STARTED_EVENT\n")
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
                    result.output shouldContain "Status: Running"
                    result.output.shouldBePublicSafe()
                    result.error shouldBe ""
                }
            }

            `when`("the requested work item is missing") {
                then("inspect missing work item fails safely") {
                    val result = runCli("inspect", "agent-task:99", "--stdin", input = "$STARTED_EVENT\n")

                    result.exitCode shouldBe 1
                    result.error shouldContain "Work item was not found."
                    result.error.shouldBePublicSafe()
                    result.output shouldBe ""
                }
            }

            `when`("the work item id is invalid and private-looking") {
                then("inspect invalid work item id fails without echoing the argument") {
                    val result = runCli("inspect", privateLinuxPath("private-token.txt"), "--stdin", input = "$STARTED_EVENT\n")

                    result.exitCode shouldBe 2
                    result.error shouldContain "Invalid work item id."
                    result.error.shouldBePublicSafe()
                    result.output shouldBe ""
                }
            }

            `when`("the work item id is session-like") {
                then("inspect rejects session-like work item ids without echoing the argument") {
                    val unsafeWorkItemId = "session:local-agent"

                    val result = runCli("inspect", unsafeWorkItemId, "--stdin", input = "$STARTED_EVENT\n")

                    result.exitCode shouldBe 2
                    result.error shouldContain "Invalid work item id."
                    result.error shouldNotContain unsafeWorkItemId
                    result.error.shouldBePublicSafe()
                    result.output shouldBe ""
                }
            }

            `when`("stdin is blank") {
                then("inspect empty stdin fails with existing empty input error") {
                    val result = runCli("inspect", "agent-task:42", "--stdin", input = " \n")

                    result.exitCode shouldBe 1
                    result.error shouldContain "No event input provided."
                    result.output shouldBe ""
                }
            }
        }
    })
