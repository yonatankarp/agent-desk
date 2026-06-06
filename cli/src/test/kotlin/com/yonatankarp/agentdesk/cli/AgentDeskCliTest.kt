package com.yonatankarp.agentdesk.cli

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import kotlin.io.path.readLines
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AgentDeskCliTest {
    @Test
    fun `sample mode is the default public-safe output`() {
        val result = runCli()

        assertEquals(0, result.exitCode)
        assertContains(result.output, "Agent Desk")
        assertContains(result.output, "sample-agent")
        assertPublicSafe(result.output)
        assertEquals("", result.error)
    }

    @Test
    fun `event file input renders projected operator state`() {
        val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
        Files.writeString(eventFile, "$STARTED_EVENT\n$BLOCKED_EVENT\n")

        val result = runCli("--events", eventFile.toString())

        assertEquals(0, result.exitCode)
        assertContains(result.output, "- [Blocked] agent-task:42 Run public hygiene check")
        assertContains(result.output, "- agent-task:42 Run public hygiene check (Blocked)")
        assertContains(result.output, "work.blocked agent-task:42 from mock-adapter")
        assertPublicSafe(result.output)
        assertEquals("", result.error)
    }

    @Test
    fun `stdin input renders projected operator state`() {
        val result = runCli("--stdin", input = "$STARTED_EVENT\n")

        assertEquals(0, result.exitCode)
        assertContains(result.output, "- [Running] agent-task:42 Run public hygiene check")
        assertContains(result.output, "Attention queue\n- none")
    }

    @Test
    fun `empty config file renders default sample state`() {
        val configFile = Files.createTempFile("agent-desk-cli-config", ".properties")
        Files.writeString(configFile, "")

        val result = runCli("--config", configFile.toString())

        assertEquals(0, result.exitCode)
        assertContains(result.output, "Agent Desk")
        assertContains(result.output, "sample-agent")
        assertPublicSafe(result.output)
        assertEquals("", result.error)
    }

    @Test
    fun `stored event config renders projected operator state`() {
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

        assertEquals(0, result.exitCode)
        assertContains(result.output, "- [Blocked] agent-task:42 Run public hygiene check")
        assertContains(result.output, "- agent-task:42 Run public hygiene check (Blocked)")
        assertContains(result.output, "work.blocked agent-task:42 from mock-adapter")
        assertPublicSafe(result.output)
        assertEquals("", result.error)
    }

    @Test
    fun `stored event config with torn trailing record renders prefix and warns on stderr`() {
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

        assertEquals(0, result.exitCode)
        assertContains(result.output, "- [Blocked] agent-task:42 Run public hygiene check")
        assertContains(result.error, "Torn trailing record at line 3")
        assertPublicSafe(result.output)
        assertPublicSafe(result.error)
        assertFalse(result.error.contains(eventFile.toString()))
    }

    @Test
    fun `inspect with torn trailing store renders prefix and warns on stderr`() {
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

        assertEquals(0, result.exitCode)
        assertContains(result.output, "Work item agent-task:42")
        assertContains(result.error, "Torn trailing record at line 2")
        assertPublicSafe(result.error)
        assertFalse(result.error.contains(eventFile.toString()))
    }

    @Test
    fun `invalid input fails without echoing raw private-looking data`() {
        val result = runCli("--stdin", input = """{"secret":"${privateLinuxPath("private-token.txt")}"}""")

        assertEquals(1, result.exitCode)
        assertContains(result.error, "Invalid event record at line 1.")
        assertPublicSafe(result.error)
        assertEquals("", result.output)
    }

    @Test
    fun `stdin input rejects channel-like work item ids without echoing them`() {
        val rawIdentifier = "123456789" + "012345678"
        val unsafeWorkItemId = "channel:$rawIdentifier"
        val unsafeEvent =
            """{"id":"event:agent-task:42:started","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter",""" +
                "\"workItemId\":\"$unsafeWorkItemId\",\"type\":\"work.started\"," +
                "\"payload\":{\"title\":\"Run public hygiene check\"}}"

        val result = runCli("--stdin", input = unsafeEvent)

        assertEquals(1, result.exitCode)
        assertContains(result.error, "Invalid event record at line 1.")
        assertFalse(result.error.contains(unsafeWorkItemId))
        assertFalse(result.error.contains(rawIdentifier))
        assertPublicSafe(result.error)
        assertEquals("", result.output)
    }

    @Test
    fun `duplicate event ids fail with a clear line-numbered error`() {
        val result = runCli("--stdin", input = "$STARTED_EVENT\n$STARTED_EVENT\n")

        assertEquals(1, result.exitCode)
        assertContains(result.error, "Duplicate work event id at line 2.")
        assertPublicSafe(result.error)
        assertEquals("", result.output)
    }

    @Test
    fun `unsupported event types fail with a clear line-numbered error`() {
        val result = runCli("--stdin", input = UNSUPPORTED_EVENT)

        assertEquals(1, result.exitCode)
        assertContains(result.error, "Unsupported event type at line 1.")
        assertEquals("", result.output)
    }

    @Test
    fun `out-of-order events fail with a clear projection error`() {
        val result = runCli("--stdin", input = BLOCKED_EVENT)

        assertEquals(1, result.exitCode)
        assertContains(result.error, "Invalid event sequence:")
        assertPublicSafe(result.error)
        assertEquals("", result.output)
    }

    @Test
    fun `empty stdin input fails with a clear error`() {
        val result = runCli("--stdin", input = " \n")

        assertEquals(1, result.exitCode)
        assertContains(result.error, "No event input provided.")
        assertEquals("", result.output)
    }

    @Test
    fun `missing event file option value is a usage error`() {
        val result = runCli("--events")

        assertEquals(2, result.exitCode)
        assertContains(result.error, "Missing value for --events.")
        assertContains(result.error, "Run with --help for usage.")
        assertEquals("", result.output)
    }

    @Test
    fun `invalid event file path fails without echoing the path`() {
        val result = runCli("--events", "\u0000${privateLinuxPath("private-token.txt")}")

        assertEquals(1, result.exitCode)
        assertContains(result.error, "Event input file could not be read.")
        assertPublicSafe(result.error)
        assertEquals("", result.output)
    }

    @Test
    fun `unknown option fails without echoing the argument`() {
        val result = runCli("--private-token-file=${privateLinuxPath("private-token.txt")}")

        assertEquals(2, result.exitCode)
        assertContains(result.error, "Unknown option.")
        assertPublicSafe(result.error)
        assertEquals("", result.output)
    }

    @Test
    fun `missing config option value is a usage error`() {
        val result = runCli("--config")

        assertEquals(2, result.exitCode)
        assertContains(result.error, "Missing value for --config.")
        assertContains(result.error, "Run with --help for usage.")
        assertEquals("", result.output)
    }

    @Test
    fun `config cannot be combined with other input modes`() {
        val configFile = Files.createTempFile("agent-desk-cli-config", ".properties")

        val result = runCli("--config", configFile.toString(), "--sample")

        assertEquals(2, result.exitCode)
        assertContains(result.error, "Choose only one input mode.")
        assertPublicSafe(result.error)
        assertEquals("", result.output)
    }

    @Test
    fun `invalid config file path fails without echoing the path`() {
        val result = runCli("--config", "\u0000${privateLinuxPath("private-token.properties")}")

        assertEquals(1, result.exitCode)
        assertContains(result.error, "Runtime config file could not be read.")
        assertPublicSafe(result.error)
        assertEquals("", result.output)
    }

    @Test
    fun `stored event config missing event store location fails safely`() {
        val configFile = Files.createTempFile("agent-desk-cli-config", ".properties")
        Files.writeString(
            configFile,
            """
            mode=stored-events
            source=local-event-store
            """.trimIndent(),
        )

        val result = runCli("--config", configFile.toString())

        assertEquals(1, result.exitCode)
        assertContains(result.error, "Invalid runtime config: stored event mode requires eventStoreLocation")
        assertPublicSafe(result.error)
        assertEquals("", result.output)
    }

    @Test
    fun `invalid runtime config rejects without echoing raw values`() {
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

        assertEquals(1, result.exitCode)
        assertContains(result.error, "Invalid runtime config:")
        assertPublicSafe(result.error)
        assertEquals("", result.output)
    }

    @Test
    fun `stored event config with invalid event store path fails safely`() {
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

        assertEquals(1, result.exitCode)
        assertContains(result.error, "Configured event store could not be read.")
        assertFalse(result.error.contains("broken.ndjson"))
        assertPublicSafe(result.error)
        assertEquals("", result.output)
    }

    @Test
    fun `stored event config unsafe event ids fail safely`() {
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

        assertEquals(1, result.exitCode)
        assertContains(result.error, "Corrupt work event record at line 1 in configured event store")
        assertFalse(result.error.contains(unsafeEventId))
        assertPublicSafe(result.error)
        assertEquals("", result.output)
    }

    @Test
    fun `mock runtime import writes canonical events that render through config`() {
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

        assertEquals(0, importResult.exitCode)
        assertContains(importResult.output, "Imported 6 mock runtime event(s); skipped 0 duplicate event(s).")
        assertContains(
            importResult.output,
            "Diagnostics: imported=6 skipped-duplicate=0 invalid=0 unsafe-rejected=0 store-rejected=0 redacted-or-dropped=0.",
        )
        assertPublicSafe(importResult.output)
        assertEquals("", importResult.error)
        assertEquals(
            listOf(
                "work.started",
                "work.started",
                "work.blocked",
                "work.started",
                "work.needs-decision",
                "work.succeeded",
            ),
            eventFile.readLines().map { it.substringAfter("\"type\":\"").substringBefore("\"") },
        )
        assertEquals(0, renderResult.exitCode)
        assertContains(renderResult.output, "- [Succeeded] agent-task:42 Run public hygiene check")
        assertContains(renderResult.output, "- [Blocked] agent-task:44 Investigate core test failure")
        assertContains(renderResult.output, "- [Needs decision] agent-task:45 Choose retry strategy")
        assertContains(renderResult.output, "work.needs-decision agent-task:45 from mock-adapter")
        assertPublicSafe(renderResult.output)
        assertEquals("", renderResult.error)
    }

    @Test
    fun `mock runtime import skips existing duplicate events`() {
        val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")

        val firstResult = runCli("import-mock-runtime", "--event-store", eventFile.toString())
        val secondResult = runCli("import-mock-runtime", "--event-store", eventFile.toString())

        assertEquals(0, firstResult.exitCode)
        assertEquals(0, secondResult.exitCode)
        assertContains(secondResult.output, "Imported 0 mock runtime event(s); skipped 6 duplicate event(s).")
        assertContains(
            secondResult.output,
            "Diagnostics: imported=0 skipped-duplicate=6 invalid=0 unsafe-rejected=0 store-rejected=0 redacted-or-dropped=0.",
        )
        assertPublicSafe(secondResult.output)
        assertEquals(6, eventFile.readLines().size)
    }

    @Test
    fun `sanitized observation import writes canonical events that render through config`() {
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

        assertEquals(0, importResult.exitCode)
        assertContains(importResult.output, "Imported 2 sanitized observation event(s); skipped 0 duplicate event(s).")
        assertContains(
            importResult.output,
            "Diagnostics: imported=2 skipped-duplicate=0 invalid=0 unsafe-rejected=0 store-rejected=0 redacted-or-dropped=0.",
        )
        assertPublicSafe(importResult.output)
        assertEquals("", importResult.error)
        assertEquals(
            listOf("work.started", "work.blocked"),
            eventFile.readLines().map { it.substringAfter("\"type\":\"").substringBefore("\"") },
        )
        assertEquals(0, renderResult.exitCode)
        assertContains(renderResult.output, "- [Blocked] agent-task:212 Run sanitized import smoke")
        assertContains(renderResult.output, "sanitized-note Runtime adapter decision")
        assertPublicSafe(renderResult.output)
        assertEquals("", renderResult.error)
    }

    @Test
    fun `sanitized observation import skips existing duplicate events`() {
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

        assertEquals(0, firstResult.exitCode)
        assertEquals(0, secondResult.exitCode)
        assertContains(secondResult.output, "Imported 0 sanitized observation event(s); skipped 2 duplicate event(s).")
        assertContains(
            secondResult.output,
            "Diagnostics: imported=0 skipped-duplicate=2 invalid=0 unsafe-rejected=0 store-rejected=0 redacted-or-dropped=0.",
        )
        assertPublicSafe(secondResult.output)
        assertEquals(2, eventFile.readLines().size)
    }

    @Test
    fun `sanitized observation import requires an observations option`() {
        val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")

        val result = runCli("import-openclaw-observations", "--event-store", eventFile.toString())

        assertEquals(2, result.exitCode)
        assertContains(result.error, "Missing value for --observations.")
        assertEquals("", result.output)
    }

    @Test
    fun `sanitized observation import rejects invalid exports without echoing paths`() {
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

        assertEquals(1, result.exitCode)
        assertContains(result.error, "Runtime observations could not be imported.")
        assertFalse(result.error.contains(observationsFile.toString()))
        assertPublicSafe(result.error)
        assertEquals("", result.output)
    }

    @Test
    fun `mock runtime import rejects unsafe event store locations`() {
        val result = runCli("import-mock-runtime", "--event-store", privateLinuxPath("private-token.ndjson"))

        assertEquals(1, result.exitCode)
        assertContains(result.error, "Invalid event store location:")
        assertPublicSafe(result.error)
        assertEquals("", result.output)
    }

    @Test
    fun `mock runtime import requires an event store option`() {
        val result = runCli("import-mock-runtime")

        assertEquals(2, result.exitCode)
        assertContains(result.error, "Missing value for --event-store.")
        assertEquals("", result.output)
    }

    @Test
    fun `mock resume action appends a sanitized result event`() {
        val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
        Files.writeString(eventFile, "$STARTED_EVENT\n$NEEDS_DECISION_EVENT\n")

        val actionResult = runCli("act", "resume", "agent-task:42", "--event-store", eventFile.toString())
        val inspectResult = runCli("inspect", "agent-task:42", "--events", eventFile.toString())

        assertEquals(0, actionResult.exitCode)
        assertContains(actionResult.output, "Recorded resume action for agent-task:42")
        assertContains(actionResult.output, "event:agent-task:42:action-resume")
        assertPublicSafe(actionResult.output)
        assertEquals("", actionResult.error)
        assertEquals(
            listOf("work.started", "work.needs-decision", "work.started"),
            eventFile.readLines().map { it.substringAfter("\"type\":\"").substringBefore("\"") },
        )
        assertEquals(0, inspectResult.exitCode)
        assertContains(inspectResult.output, "Status: Running")
        assertContains(inspectResult.output, "mock-action-adapter")
        assertPublicSafe(inspectResult.output)
    }

    @Test
    fun `mock action rejects disallowed intent safely`() {
        val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
        Files.writeString(eventFile, "$STARTED_EVENT\n$NEEDS_DECISION_EVENT\n")

        val result = runCli("act", "stop", "agent-task:42", "--event-store", eventFile.toString())

        assertEquals(1, result.exitCode)
        assertContains(result.error, "Mock action adapter currently supports only resume.")
        assertPublicSafe(result.error)
        assertEquals("", result.output)
    }

    @Test
    fun `mock action rejects missing work item safely`() {
        val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
        Files.writeString(eventFile, "$STARTED_EVENT\n$NEEDS_DECISION_EVENT\n")

        val result = runCli("act", "resume", "agent-task:99", "--event-store", eventFile.toString())

        assertEquals(1, result.exitCode)
        assertContains(result.error, "Work item was not found.")
        assertPublicSafe(result.error)
        assertEquals("", result.output)
    }

    @Test
    fun `mock action rejects unsafe event store location`() {
        val result = runCli("act", "resume", "agent-task:42", "--event-store", privateLinuxPath("private-token.ndjson"))

        assertEquals(1, result.exitCode)
        assertContains(result.error, "Invalid event store location:")
        assertPublicSafe(result.error)
        assertEquals("", result.output)
    }

    @Test
    fun `inspect sample item renders item details`() {
        val result = runCli("inspect", "agent-task:43")

        assertEquals(0, result.exitCode)
        assertContains(result.output, "Work item agent-task:43")
        assertContains(result.output, "Status: Needs decision")
        assertContains(result.output, "Title: Choose adapter boundary")
        assertContains(result.output, "Attention: yes")
        assertContains(result.output, "Accepted recent events")
        assertContains(result.output, "Projection warnings\n- none")
        assertContains(result.output, "Evidence references\n- none")
        assertPublicSafe(result.output)
        assertEquals("", result.error)
    }

    @Test
    fun `inspect event file renders only selected work item events`() {
        val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
        Files.writeString(eventFile, "$STARTED_EVENT\n$BLOCKED_EVENT\n$OTHER_STARTED_EVENT\n")

        val result = runCli("inspect", "agent-task:42", "--events", eventFile.toString())

        assertEquals(0, result.exitCode)
        assertContains(result.output, "Work item agent-task:42")
        assertContains(result.output, "Status: Blocked")
        assertContains(result.output, "Summary: CI failed on the core test task.")
        assertContains(result.output, "work.started from mock-adapter")
        assertContains(result.output, "work.blocked from mock-adapter")
        assertFalse(result.output.contains("Prepare release checklist"))
        assertPublicSafe(result.output)
        assertEquals("", result.error)
    }

    @Test
    fun `inspect stdin renders terminal item and projection warnings`() {
        val result = runCli(
            "inspect",
            "agent-task:42",
            "--stdin",
            input = "$STARTED_EVENT\n$SUCCEEDED_EVENT\n$BLOCKED_EVENT\n",
        )

        assertEquals(0, result.exitCode)
        assertContains(result.output, "Status: Succeeded")
        assertContains(result.output, "Terminal: yes")
        assertContains(result.output, "Projection warnings")
        assertContains(result.output, "ignored event - Cannot transition work item agent-task:42")
        assertPublicSafe(result.output)
        assertEquals("", result.error)
    }

    @Test
    fun `inspect stored event config uses configured input mode`() {
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

        assertEquals(0, result.exitCode)
        assertContains(result.output, "Work item agent-task:42")
        assertContains(result.output, "Status: Running")
        assertPublicSafe(result.output)
        assertEquals("", result.error)
    }

    @Test
    fun `inspect missing work item fails safely`() {
        val result = runCli("inspect", "agent-task:99", "--stdin", input = "$STARTED_EVENT\n")

        assertEquals(1, result.exitCode)
        assertContains(result.error, "Work item was not found.")
        assertPublicSafe(result.error)
        assertEquals("", result.output)
    }

    @Test
    fun `inspect invalid work item id fails without echoing the argument`() {
        val result = runCli("inspect", privateLinuxPath("private-token.txt"), "--stdin", input = "$STARTED_EVENT\n")

        assertEquals(2, result.exitCode)
        assertContains(result.error, "Invalid work item id.")
        assertPublicSafe(result.error)
        assertEquals("", result.output)
    }

    @Test
    fun `inspect rejects session-like work item ids without echoing the argument`() {
        val unsafeWorkItemId = "session:local-agent"

        val result = runCli("inspect", unsafeWorkItemId, "--stdin", input = "$STARTED_EVENT\n")

        assertEquals(2, result.exitCode)
        assertContains(result.error, "Invalid work item id.")
        assertFalse(result.error.contains(unsafeWorkItemId))
        assertPublicSafe(result.error)
        assertEquals("", result.output)
    }

    @Test
    fun `inspect empty stdin fails with existing empty input error`() {
        val result = runCli("inspect", "agent-task:42", "--stdin", input = " \n")

        assertEquals(1, result.exitCode)
        assertContains(result.error, "No event input provided.")
        assertEquals("", result.output)
    }

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
            )

        return CliRunResult(
            exitCode = exitCode,
            output = output.toString().trimEnd(),
            error = error.toString().trimEnd(),
        )
    }

    private fun assertPublicSafe(text: String) {
        val rawIdentifier = "123456789" + "012345678"

        assertFalse(text.contains("/home/"))
        assertFalse(text.contains("/Users/"))
        assertFalse(text.contains("\\Users\\"))
        assertFalse(text.contains("file:", ignoreCase = true))
        assertFalse(text.contains("private-token"))
        assertFalse(text.contains("discord", ignoreCase = true))
        assertFalse(text.contains("channel:", ignoreCase = true))
        assertFalse(text.contains("message:", ignoreCase = true))
        assertFalse(text.contains("session:", ignoreCase = true))
        assertFalse(text.contains("thread:", ignoreCase = true))
        assertFalse(text.contains("raw transcript", ignoreCase = true))
        assertFalse(text.contains("bearer", ignoreCase = true))
        assertFalse(text.contains("auth_token", ignoreCase = true))
        assertFalse(text.contains("github_pat_", ignoreCase = true))
        assertFalse(text.contains("ghp_", ignoreCase = true))
        assertFalse(text.contains("op://", ignoreCase = true))
        assertFalse(text.contains("password", ignoreCase = true))
        assertFalse(text.contains("secret", ignoreCase = true))
        assertFalse(text.contains("xoxb-", ignoreCase = true))
        assertFalse(text.contains(rawIdentifier))
    }

    private fun privateLinuxPath(fileName: String): String = "/home/" + "operator/$fileName"

    private data class CliRunResult(
        val exitCode: Int,
        val output: String,
        val error: String,
    )

    companion object {
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
