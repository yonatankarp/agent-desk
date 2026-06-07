package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import kotlin.io.path.readLines

class AgentDeskCliTest :
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

        given("usage errors") {
            `when`("each invalid argument list is run") {
                then("it exits with code 2, explains the error, and prints nothing on stdout") {
                    usageErrorCases().forEach { case ->
                        val result = runCli(*case.args.toTypedArray())

                        withClue("args: ${case.args}") {
                            result.exitCode shouldBe 2
                            case.expectedErrors.forEach { expected ->
                                result.error shouldContain expected
                            }
                            result.error.shouldBePublicSafe()
                            result.output shouldBe ""
                        }
                    }
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

        given("the help flags") {
            `when`("each is run") {
                then("help flags print usage and exit cleanly") {
                    listOf("--help", "-h").forEach { flag ->
                        val result = runCli(flag)

                        withClue("args: $flag") {
                            result.exitCode shouldBe 0
                            result.output shouldContain "Agent Desk"
                            result.output shouldContain "Usage:"
                            result.output shouldContain "--help          Show this help."
                            result.output.shouldBePublicSafe()
                            result.error shouldBe ""
                        }
                    }
                }
            }
        }

        given("a config combined with another input mode") {
            `when`("the CLI is run") {
                then("config cannot be combined with other input modes") {
                    val configFile = Files.createTempFile("agent-desk-cli-config", ".properties")

                    val result = runCli("--config", configFile.toString(), "--sample")

                    result.exitCode shouldBe 2
                    result.error shouldContain "Choose only one input mode."
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

        given("the mock runtime import command") {
            `when`("events are imported and then rendered through config") {
                then("mock runtime import writes canonical events that render through config") {
                    val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
                    val configFile = Files.createTempFile("agent-desk-cli-config", ".properties")

                    val importResult = runCli("import-mock-runtime", "--event-store", eventFile.toString())
                    Files.writeString(
                        configFile,
                        """
                        mode=stored-events
                        source=local-event-store
                        eventStoreLocation=$eventFile
                        """.trimIndent(),
                    )
                    val renderResult = runCli("--config", configFile.toString())

                    importResult.exitCode shouldBe 0
                    importResult.output shouldContain "Imported 6 mock runtime event(s); skipped 0 duplicate event(s)."
                    importResult.output shouldContain
                        "Diagnostics: imported=6 skipped-duplicate=0 invalid=0 unsafe-rejected=0 store-rejected=0 redacted-or-dropped=0."
                    importResult.output.shouldBePublicSafe()
                    importResult.error shouldBe ""
                    eventFile.readLines().map { it.substringAfter("\"type\":\"").substringBefore("\"") } shouldBe
                        listOf(
                            "work.started",
                            "work.started",
                            "work.blocked",
                            "work.started",
                            "work.needs-decision",
                            "work.succeeded",
                        )
                    renderResult.exitCode shouldBe 0
                    renderResult.output shouldContain "- [Succeeded] agent-task:42 Run public hygiene check"
                    renderResult.output shouldContain "- [Blocked] agent-task:44 Investigate core test failure"
                    renderResult.output shouldContain "- [Needs decision] agent-task:45 Choose retry strategy"
                    renderResult.output shouldContain "work.needs-decision agent-task:45 from mock-adapter"
                    renderResult.output.shouldBePublicSafe()
                    renderResult.error shouldBe ""
                }
            }

            `when`("the same import runs twice into the same store") {
                then("mock runtime import skips existing duplicate events") {
                    val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")

                    val firstResult = runCli("import-mock-runtime", "--event-store", eventFile.toString())
                    val secondResult = runCli("import-mock-runtime", "--event-store", eventFile.toString())

                    firstResult.exitCode shouldBe 0
                    secondResult.exitCode shouldBe 0
                    secondResult.output shouldContain "Imported 0 mock runtime event(s); skipped 6 duplicate event(s)."
                    secondResult.output shouldContain
                        "Diagnostics: imported=0 skipped-duplicate=6 invalid=0 unsafe-rejected=0 store-rejected=0 redacted-or-dropped=0."
                    secondResult.output.shouldBePublicSafe()
                    eventFile.readLines().size shouldBe 6
                }
            }

            `when`("an unsafe event store location is requested") {
                then("mock runtime import rejects unsafe event store locations") {
                    val result = runCli("import-mock-runtime", "--event-store", privateLinuxPath("private-token.ndjson"))

                    result.exitCode shouldBe 1
                    result.error shouldContain "Invalid event store location:"
                    result.error.shouldBePublicSafe()
                    result.output shouldBe ""
                }
            }
        }

        given("the sanitized observation import command") {
            `when`("a sanitized export is imported and then rendered through config") {
                then("sanitized observation import writes canonical events that render through config") {
                    val observationsFile = Files.createTempFile("agent-desk-cli-observations", ".json")
                    val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
                    val configFile = Files.createTempFile("agent-desk-cli-config", ".properties")
                    Files.writeString(observationsFile, SANITIZED_OBSERVATION_EXPORT)

                    val importResult = runCli(
                        "import-openclaw-observations",
                        "--observations",
                        observationsFile.toString(),
                        "--event-store",
                        eventFile.toString(),
                    )
                    Files.writeString(
                        configFile,
                        """
                        mode=stored-events
                        source=local-event-store
                        eventStoreLocation=$eventFile
                        """.trimIndent(),
                    )
                    val renderResult = runCli("--config", configFile.toString())

                    importResult.exitCode shouldBe 0
                    importResult.output shouldContain "Imported 2 sanitized observation event(s); skipped 0 duplicate event(s)."
                    importResult.output shouldContain
                        "Diagnostics: imported=2 skipped-duplicate=0 invalid=0 unsafe-rejected=0 store-rejected=0 redacted-or-dropped=0."
                    importResult.output.shouldBePublicSafe()
                    importResult.error shouldBe ""
                    eventFile.readLines().map { it.substringAfter("\"type\":\"").substringBefore("\"") } shouldBe
                        listOf("work.started", "work.blocked")
                    renderResult.exitCode shouldBe 0
                    renderResult.output shouldContain "- [Blocked] agent-task:212 Run sanitized import smoke"
                    renderResult.output shouldContain "sanitized-note Runtime adapter decision"
                    renderResult.output.shouldBePublicSafe()
                    renderResult.error shouldBe ""
                }
            }

            `when`("the same sanitized export is imported twice into the same store") {
                then("sanitized observation import skips existing duplicate events") {
                    val observationsFile = Files.createTempFile("agent-desk-cli-observations", ".json")
                    val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
                    Files.writeString(observationsFile, SANITIZED_OBSERVATION_EXPORT)

                    val firstResult = runCli(
                        "import-openclaw-observations",
                        "--observations",
                        observationsFile.toString(),
                        "--event-store",
                        eventFile.toString(),
                    )
                    val secondResult = runCli(
                        "import-openclaw-observations",
                        "--observations",
                        observationsFile.toString(),
                        "--event-store",
                        eventFile.toString(),
                    )

                    firstResult.exitCode shouldBe 0
                    secondResult.exitCode shouldBe 0
                    secondResult.output shouldContain "Imported 0 sanitized observation event(s); skipped 2 duplicate event(s)."
                    secondResult.output shouldContain
                        "Diagnostics: imported=0 skipped-duplicate=2 invalid=0 unsafe-rejected=0 store-rejected=0 redacted-or-dropped=0."
                    secondResult.output.shouldBePublicSafe()
                    eventFile.readLines().size shouldBe 2
                }
            }

            `when`("no observations option is provided") {
                then("sanitized observation import requires an observations option") {
                    val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")

                    val result = runCli("import-openclaw-observations", "--event-store", eventFile.toString())

                    result.exitCode shouldBe 2
                    result.error shouldContain "Missing value for --observations."
                    result.output shouldBe ""
                }
            }

            `when`("the export is invalid and private-looking") {
                then("sanitized observation import rejects invalid exports without echoing paths") {
                    val observationsFile = Files.createTempFile("agent-desk-cli-observations", ".json")
                    val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
                    Files.writeString(observationsFile, """{"rawTranscript":"${privateLinuxPath("private-token.txt")}"}""")

                    val result = runCli(
                        "import-openclaw-observations",
                        "--observations",
                        observationsFile.toString(),
                        "--event-store",
                        eventFile.toString(),
                    )

                    result.exitCode shouldBe 1
                    result.error shouldContain "Runtime observations could not be imported."
                    result.error shouldNotContain observationsFile.toString()
                    result.error.shouldBePublicSafe()
                    result.output shouldBe ""
                }
            }
        }

        given("the mock act command") {
            `when`("an approved resume routes through the gate") {
                then("the decision and audit evidence are durably recorded with the result event") {
                    val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
                    val auditFile = Files.createTempFile("agent-desk-cli-audit", ".ndjson")
                    Files.writeString(eventFile, "$STARTED_EVENT\n$NEEDS_DECISION_EVENT\n")

                    val actionResult = runCli(
                        "act",
                        "resume",
                        "agent-task:42",
                        "--event-store",
                        eventFile.toString(),
                        "--audit-store",
                        auditFile.toString(),
                        "--approve",
                    )
                    val inspectResult = runCli("inspect", "agent-task:42", "--events", eventFile.toString())

                    actionResult.exitCode shouldBe 0
                    actionResult.output shouldContain "Permission decision"
                    actionResult.output shouldContain "- Approved"
                    actionResult.output shouldContain
                        "Recorded event: event:agent-task:42:action-resume:2026-06-06t09:30:00z"
                    actionResult.output shouldContain "Audit trail (3 durable record(s))"
                    actionResult.output shouldContain "permission.localwrite"
                    actionResult.output shouldContain "mock.resume"
                    actionResult.output.shouldBePublicSafe()
                    actionResult.error shouldBe ""
                    eventFile.readLines().map { it.substringAfter("\"type\":\"").substringBefore("\"") } shouldBe
                        listOf("work.started", "work.needs-decision", "work.started")
                    val auditRecords = auditFile.readLines()
                    auditRecords shouldHaveSize 3
                    auditRecords.joinToString("\n").let { trail ->
                        trail shouldContain "\"action\":\"permission.localwrite\""
                        trail shouldContain "\"action\":\"mock.resume\""
                        trail shouldContain "\"actor\":\"operator:cli\""
                        trail.shouldBePublicSafe()
                    }
                    inspectResult.exitCode shouldBe 0
                    inspectResult.output shouldContain "Status: Running"
                    inspectResult.output.shouldBePublicSafe()
                }
            }

            `when`("a resume is requested without --approve") {
                then("the gate denies, the denial is audited, and the exit code reports policy denial") {
                    val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
                    val auditFile = Files.createTempFile("agent-desk-cli-audit", ".ndjson")
                    Files.writeString(eventFile, "$STARTED_EVENT\n$NEEDS_DECISION_EVENT\n")

                    val result = runCli(
                        "act",
                        "resume",
                        "agent-task:42",
                        "--event-store",
                        eventFile.toString(),
                        "--audit-store",
                        auditFile.toString(),
                    )

                    result.exitCode shouldBe 3
                    result.output shouldContain "- Denied"
                    result.output shouldContain "approval is required"
                    result.output shouldContain "No action was recorded. Audit evidence was still written."
                    result.output shouldContain "Re-run with --approve"
                    result.output.shouldBePublicSafe()
                    eventFile.readLines() shouldHaveSize 2
                    val auditRecords = auditFile.readLines()
                    auditRecords shouldHaveSize 1
                    auditRecords.single() shouldContain "\"result\":\"rejected\""
                    auditRecords.single().shouldBePublicSafe()
                }
            }

            `when`("a destructive stop is requested even with --approve") {
                then("the gate fails closed and the denial is still audited") {
                    val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
                    val auditFile = Files.createTempFile("agent-desk-cli-audit", ".ndjson")
                    Files.writeString(eventFile, "$STARTED_EVENT\n$NEEDS_DECISION_EVENT\n")

                    val result = runCli(
                        "act",
                        "stop",
                        "agent-task:42",
                        "--event-store",
                        eventFile.toString(),
                        "--audit-store",
                        auditFile.toString(),
                        "--approve",
                    )

                    result.exitCode shouldBe 3
                    result.output shouldContain "- Denied"
                    result.output shouldContain "No action was recorded. Audit evidence was still written."
                    result.output shouldContain "unavailable for the work item"
                    result.output.shouldBePublicSafe()
                    eventFile.readLines() shouldHaveSize 2
                    auditFile.readLines().single().shouldBePublicSafe()
                }
            }

            `when`("the target work item is missing") {
                then("the act fails pre-gate without writing audit records") {
                    val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
                    val auditFile = Files.createTempFile("agent-desk-cli-audit", ".ndjson")
                    Files.writeString(eventFile, "$STARTED_EVENT\n$NEEDS_DECISION_EVENT\n")

                    val result = runCli(
                        "act",
                        "resume",
                        "agent-task:99",
                        "--event-store",
                        eventFile.toString(),
                        "--audit-store",
                        auditFile.toString(),
                        "--approve",
                    )

                    result.exitCode shouldBe 1
                    result.error shouldContain "Work item was not found."
                    result.error.shouldBePublicSafe()
                    result.output shouldBe ""
                    auditFile.readLines() shouldHaveSize 0
                }
            }

            `when`("an unsafe event store location is requested") {
                then("mock action rejects unsafe event store location") {
                    val auditFile = Files.createTempFile("agent-desk-cli-audit", ".ndjson")

                    val result = runCli(
                        "act",
                        "resume",
                        "agent-task:42",
                        "--event-store",
                        privateLinuxPath("private-token.ndjson"),
                        "--audit-store",
                        auditFile.toString(),
                    )

                    result.exitCode shouldBe 1
                    result.error shouldContain "Invalid event store location:"
                    result.error.shouldBePublicSafe()
                    result.output shouldBe ""
                }
            }

            `when`("an unsafe audit store location is requested") {
                then("mock action rejects unsafe audit store location") {
                    val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
                    Files.writeString(eventFile, "$STARTED_EVENT\n$NEEDS_DECISION_EVENT\n")

                    val result = runCli(
                        "act",
                        "resume",
                        "agent-task:42",
                        "--event-store",
                        eventFile.toString(),
                        "--audit-store",
                        privateLinuxPath("private-token.ndjson"),
                    )

                    result.exitCode shouldBe 1
                    result.error shouldContain "Invalid audit store location:"
                    result.error.shouldBePublicSafe()
                    result.output shouldBe ""
                }
            }
        }

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
    }) {
    private data class CliRunResult(
        val exitCode: Int,
        val output: String,
        val error: String,
    )

    private data class UsageErrorCase(
        val args: List<String>,
        val expectedErrors: List<String>,
    )

    companion object {
        private fun runCli(
            vararg args: String,
            input: String = "",
        ): CliRunResult {
            val output = ByteArrayOutputStream()
            val error = ByteArrayOutputStream()
            val exitCode =
                AgentDeskCli.run(
                    args = args.toList().toTypedArray(),
                    input = ByteArrayInputStream(input.encodeToByteArray()),
                    output = PrintStream(output),
                    error = PrintStream(error),
                    now = { EventTimestamp.parse("2026-06-06T09:30:00Z") },
                )

            return CliRunResult(
                exitCode = exitCode,
                output = output.toString().trimEnd(),
                error = error.toString().trimEnd(),
            )
        }

        private fun privateLinuxPath(fileName: String): String = "/home/" + "operator/$fileName"

        private fun usageErrorCases(): List<UsageErrorCase> = listOf(
            UsageErrorCase(
                args = listOf("--events"),
                expectedErrors = listOf("Missing value for --events.", "Run with --help for usage."),
            ),
            UsageErrorCase(
                args = listOf("--config"),
                expectedErrors = listOf("Missing value for --config.", "Run with --help for usage."),
            ),
            UsageErrorCase(
                args = listOf("--private-token-file=${privateLinuxPath("private-token.txt")}"),
                expectedErrors = listOf("Unknown option."),
            ),
            UsageErrorCase(
                args = listOf("inspect", "agent-task:42", "import-mock-runtime"),
                expectedErrors = listOf("Choose only one command.", "Run with --help for usage."),
            ),
            UsageErrorCase(
                args = listOf("import-mock-runtime", "import-openclaw-observations"),
                expectedErrors = listOf("Choose only one command.", "Run with --help for usage."),
            ),
            UsageErrorCase(
                args = listOf("import-openclaw-observations", "act", "resume", "agent-task:42"),
                expectedErrors = listOf("Choose only one command.", "Run with --help for usage."),
            ),
            UsageErrorCase(
                args = listOf("act", "resume", "agent-task:42", "inspect", "agent-task:43"),
                expectedErrors = listOf("Choose only one command.", "Run with --help for usage."),
            ),
            UsageErrorCase(
                args = listOf("import-mock-runtime", "--event-store", "store-one.ndjson", "--event-store", "store-two.ndjson"),
                expectedErrors = listOf("Choose only one event store."),
            ),
            UsageErrorCase(
                args = listOf("act", "resume", "agent-task:42", "--event-store", "store-one.ndjson", "--event-store", "store-two.ndjson"),
                expectedErrors = listOf("Choose only one event store."),
            ),
            UsageErrorCase(
                args = listOf("import-openclaw-observations", "--event-store", "store-one.ndjson", "--event-store", "store-two.ndjson"),
                expectedErrors = listOf("Choose only one event store."),
            ),
            UsageErrorCase(
                args = listOf(
                    "import-openclaw-observations",
                    "--observations",
                    "observations-one.json",
                    "--observations",
                    "observations-two.json",
                ),
                expectedErrors = listOf("Choose only one observations export."),
            ),
            UsageErrorCase(
                args = listOf("--event-store", "store.ndjson"),
                expectedErrors = listOf("--event-store is only valid with import commands or act."),
            ),
            UsageErrorCase(
                args = listOf("inspect", "agent-task:42", "--event-store", "store.ndjson"),
                expectedErrors = listOf("--event-store is only valid with import commands or act."),
            ),
            UsageErrorCase(
                args = listOf("--observations", "observations.json"),
                expectedErrors = listOf("--observations is only valid with import-openclaw-observations."),
            ),
            UsageErrorCase(
                args = listOf("import-mock-runtime", "--observations", "observations.json"),
                expectedErrors = listOf("--observations is only valid with import-openclaw-observations."),
            ),
            UsageErrorCase(
                args = listOf("act", "resume", "agent-task:42", "--observations", "observations.json"),
                expectedErrors = listOf("--observations is only valid with import-openclaw-observations."),
            ),
            UsageErrorCase(
                args = listOf("act"),
                expectedErrors = listOf("Missing action intent for act."),
            ),
            UsageErrorCase(
                args = listOf("act", "--event-store", "store.ndjson"),
                expectedErrors = listOf("Missing action intent for act."),
            ),
            UsageErrorCase(
                args = listOf("act", "resume"),
                expectedErrors = listOf("Missing work item id for act."),
            ),
            UsageErrorCase(
                args = listOf("act", "resume", "--event-store"),
                expectedErrors = listOf("Missing work item id for act."),
            ),
            UsageErrorCase(
                args = listOf("act", "resume", "agent-task:42", "--event-store", "store.ndjson"),
                expectedErrors = listOf("Missing value for --audit-store."),
            ),
            UsageErrorCase(
                args = listOf(
                    "act", "resume", "agent-task:42",
                    "--event-store", "store.ndjson",
                    "--audit-store", "audit-one.ndjson",
                    "--audit-store", "audit-two.ndjson",
                ),
                expectedErrors = listOf("Choose only one audit store."),
            ),
            UsageErrorCase(
                args = listOf("--audit-store", "audit.ndjson"),
                expectedErrors = listOf("--audit-store is only valid with act."),
            ),
            UsageErrorCase(
                args = listOf("import-mock-runtime", "--audit-store", "audit.ndjson"),
                expectedErrors = listOf("--audit-store is only valid with act."),
            ),
            UsageErrorCase(
                args = listOf("--approve"),
                expectedErrors = listOf("--approve is only valid with act."),
            ),
            UsageErrorCase(
                args = listOf("inspect", "agent-task:42", "--approve"),
                expectedErrors = listOf("--approve is only valid with act."),
            ),
            UsageErrorCase(
                args = listOf("inspect"),
                expectedErrors = listOf("Missing work item id for inspect."),
            ),
            UsageErrorCase(
                args = listOf("inspect", "--sample"),
                expectedErrors = listOf("Missing work item id for inspect."),
            ),
            UsageErrorCase(
                args = listOf("import-mock-runtime"),
                expectedErrors = listOf("Missing value for --event-store."),
            ),
        )

        private const val STARTED_EVENT =
            """{"id":"event:agent-task:42:started","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.started","payload":{"title":"Run public hygiene check","summary":"Agent accepted the task and started local checks."}}"""

        private const val BLOCKED_EVENT =
            """{"id":"event:agent-task:42:blocked","occurredAt":"2026-06-02T21:05:00.123Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.blocked","payload":{"reason":"CI failed on the core test task."}}"""

        private const val NEEDS_DECISION_EVENT =
            """{"id":"event:agent-task:42:needs-decision","occurredAt":"2026-06-02T21:03:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.needs-decision","payload":{"reason":"Operator decision needed."}}"""

        private const val SUCCEEDED_EVENT =
            """{"id":"event:agent-task:42:succeeded","occurredAt":"2026-06-02T21:10:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.succeeded","payload":{}}"""

        private const val OTHER_STARTED_EVENT =
            """{"id":"event:agent-task:43:started","occurredAt":"2026-06-02T21:01:00Z","source":"mock-adapter","workItemId":"agent-task:43","type":"work.started","payload":{"title":"Prepare release checklist","summary":"Agent started release preparation."}}"""

        private const val UNSUPPORTED_EVENT =
            """{"id":"event:agent-task:42:paused","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.paused","payload":{}}"""

        private const val SANITIZED_OBSERVATION_EXPORT =
            """
            {
              "schemaVersion": 1,
              "observations": [
                {
                  "eventId": "event:agent-task:212:started",
                  "occurredAt": "2026-06-05T18:40:00Z",
                  "source": "openclaw-local",
                  "workItemId": "agent-task:212",
                  "kind": "started",
                  "title": "Run sanitized import smoke",
                  "summary": "Agent started a public-safe smoke command.",
                  "evidenceReferences": [
                    {
                      "kind": "sanitized-note",
                      "label": "Runtime adapter decision",
                      "target": "docs/runtime-adapter-scope-decision.md"
                    }
                  ]
                },
                {
                  "eventId": "event:agent-task:212:blocked",
                  "occurredAt": "2026-06-05T18:41:00Z",
                  "source": "openclaw-local",
                  "workItemId": "agent-task:212",
                  "kind": "blocked",
                  "reason": "Waiting for smoke command evidence."
                }
              ]
            }
            """
    }
}
