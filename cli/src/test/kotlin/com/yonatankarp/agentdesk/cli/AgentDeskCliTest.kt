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
    fun `invalid input fails without echoing raw private-looking data`() {
        val result = runCli("--stdin", input = """{"secret":"/home/operator/private-token.txt"}""")

        assertEquals(1, result.exitCode)
        assertContains(result.error, "Invalid event record at line 1.")
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
        val result = runCli("--events", "\u0000/home/operator/private-token.txt")

        assertEquals(1, result.exitCode)
        assertContains(result.error, "Event input file could not be read.")
        assertPublicSafe(result.error)
        assertEquals("", result.output)
    }

    @Test
    fun `unknown option fails without echoing the argument`() {
        val result = runCli("--private-token-file=/home/operator/private-token.txt")

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
        val result = runCli("--config", "\u0000/home/operator/private-token.properties")

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
            eventStoreLocation=/home/operator/private-token.ndjson
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
    fun `stored event config duplicate private-looking event ids fail safely`() {
        val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
        val configFile = Files.createTempFile("agent-desk-cli-config", ".properties")
        val privateLookingEvent =
            """{"id":"event:private-token:started","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.started","payload":{"title":"Run public hygiene check","summary":"Agent accepted the task and started local checks."}}"""
        Files.writeString(eventFile, "$privateLookingEvent\n$privateLookingEvent\n")
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
        assertContains(result.error, "Configured event store contains a duplicate work event id at line 2.")
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
        assertContains(renderResult.output, "- [NeedsDecision] agent-task:45 Choose retry strategy")
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
        assertPublicSafe(secondResult.output)
        assertEquals(6, eventFile.readLines().size)
    }

    @Test
    fun `mock runtime import rejects unsafe event store locations`() {
        val result = runCli("import-mock-runtime", "--event-store", "/home/operator/private-token.ndjson")

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
        val result = runCli("inspect", "/home/operator/private-token.txt", "--stdin", input = "$STARTED_EVENT\n")

        assertEquals(2, result.exitCode)
        assertContains(result.error, "Invalid work item id.")
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
        assertFalse(text.contains("/home/"))
        assertFalse(text.contains("private-token"))
        assertFalse(text.contains("discord", ignoreCase = true))
        assertFalse(text.contains("op://", ignoreCase = true))
    }

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

        private const val SUCCEEDED_EVENT =
            """{"id":"event:agent-task:42:succeeded","occurredAt":"2026-06-02T21:10:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.succeeded","payload":{}}"""

        private const val OTHER_STARTED_EVENT =
            """{"id":"event:agent-task:43:started","occurredAt":"2026-06-02T21:01:00Z","source":"mock-adapter","workItemId":"agent-task:43","type":"work.started","payload":{"title":"Prepare release checklist","summary":"Agent started release preparation."}}"""

        private const val UNSUPPORTED_EVENT =
            """{"id":"event:agent-task:42:paused","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.paused","payload":{}}"""
    }
}
