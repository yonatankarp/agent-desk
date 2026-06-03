package com.yonatankarp.agentdesk.cli

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import kotlin.test.Test

class AgentDeskCliTest {
    @Test
    fun `sample mode is the default public-safe output`() {
        val result = runCli()

        result.exitCode shouldBe 0
        result.output shouldContain "Agent Desk"
        result.output shouldContain "sample-agent"
        assertPublicSafe(result.output)
        result.error shouldBe ""
    }

    @Test
    fun `event file input renders projected operator state`() {
        val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
        Files.writeString(eventFile, "$STARTED_EVENT\n$BLOCKED_EVENT\n")

        val result = runCli("--events", eventFile.toString())

        result.exitCode shouldBe 0
        result.output shouldContain "- [Blocked] agent-task:42 Run public hygiene check"
        result.output shouldContain "- agent-task:42 Run public hygiene check (Blocked)"
        result.output shouldContain "work.blocked agent-task:42 from mock-adapter"
        assertPublicSafe(result.output)
        result.error shouldBe ""
    }

    @Test
    fun `stdin input renders projected operator state`() {
        val result = runCli("--stdin", input = "$STARTED_EVENT\n")

        result.exitCode shouldBe 0
        result.output shouldContain "- [Running] agent-task:42 Run public hygiene check"
        result.output shouldContain "Attention queue\n- none"
    }

    @Test
    fun `runtime config defaults render sample state`() {
        val configFile = Files.createTempFile("agent-desk-cli-config", ".properties")
        Files.writeString(configFile, "")

        val result = runCli("--config", configFile.toString())

        result.exitCode shouldBe 0
        result.output shouldContain "Agent Desk"
        result.output shouldContain "sample-agent"
        assertPublicSafe(result.output)
        result.error shouldBe ""
    }

    @Test
    fun `stored event runtime config renders projected operator state`() {
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
        result.output shouldContain "work.blocked agent-task:42 from mock-adapter"
        assertPublicSafe(result.output)
        result.error shouldBe ""
    }

    @Test
    fun `invalid runtime config fails with public-safe validation error`() {
        val configFile = Files.createTempFile("agent-desk-cli-config", ".properties")
        Files.writeString(
            configFile,
            """
            mode=private-mode
            source=private-source
            eventStoreLocation=/home/operator/private-token.ndjson
            """.trimIndent(),
        )

        val result = runCli("--config", configFile.toString())

        result.exitCode shouldBe 1
        result.error shouldContain "mode must be sample or stored-events"
        assertPublicSafe(result.error)
        result.output shouldBe ""
    }

    @Test
    fun `stored event runtime config requires an event store location`() {
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
        result.error shouldContain "stored event mode requires eventStoreLocation"
        assertPublicSafe(result.error)
        result.output shouldBe ""
    }

    @Test
    fun `unreadable configured event store fails without echoing the location`() {
        val configFile = Files.createTempFile("agent-desk-cli-config", ".properties")
        Files.writeString(
            configFile,
            """
            mode=stored-events
            source=local-event-store
            eventStoreLocation=bad${"\u0000"}store.ndjson
            """.trimIndent(),
        )

        val result = runCli("--config", configFile.toString())

        result.exitCode shouldBe 1
        result.error shouldContain "Configured event store could not be read."
        assertPublicSafe(result.error)
        result.output shouldBe ""
    }

    @Test
    fun `invalid input fails without echoing raw private-looking data`() {
        val result = runCli("--stdin", input = """{"secret":"/home/operator/private-token.txt"}""")

        result.exitCode shouldBe 1
        result.error shouldContain "Invalid event record at line 1."
        assertPublicSafe(result.error)
        result.output shouldBe ""
    }

    @Test
    fun `duplicate event ids fail with a clear line-numbered error`() {
        val result = runCli("--stdin", input = "$STARTED_EVENT\n$STARTED_EVENT\n")

        result.exitCode shouldBe 1
        result.error shouldContain "Duplicate work event id at line 2."
        assertPublicSafe(result.error)
        result.output shouldBe ""
    }

    @Test
    fun `unsupported event types fail with a clear line-numbered error`() {
        val result = runCli("--stdin", input = UNSUPPORTED_EVENT)

        result.exitCode shouldBe 1
        result.error shouldContain "Unsupported event type at line 1."
        result.output shouldBe ""
    }

    @Test
    fun `out-of-order events fail with a clear projection error`() {
        val result = runCli("--stdin", input = BLOCKED_EVENT)

        result.exitCode shouldBe 1
        result.error shouldContain "Invalid event sequence:"
        assertPublicSafe(result.error)
        result.output shouldBe ""
    }

    @Test
    fun `empty stdin input fails with a clear error`() {
        val result = runCli("--stdin", input = " \n")

        result.exitCode shouldBe 1
        result.error shouldContain "No event input provided."
        result.output shouldBe ""
    }

    @Test
    fun `missing event file option value is a usage error`() {
        val result = runCli("--events")

        result.exitCode shouldBe 2
        result.error shouldContain "Missing value for --events."
        result.error shouldContain "Run with --help for usage."
        result.output shouldBe ""
    }

    @Test
    fun `missing config file option value is a usage error`() {
        val result = runCli("--config")

        result.exitCode shouldBe 2
        result.error shouldContain "Missing value for --config."
        result.error shouldContain "Run with --help for usage."
        result.output shouldBe ""
    }

    @Test
    fun `invalid event file path fails without echoing the path`() {
        val result = runCli("--events", "\u0000/home/operator/private-token.txt")

        result.exitCode shouldBe 1
        result.error shouldContain "Event input file could not be read."
        assertPublicSafe(result.error)
        result.output shouldBe ""
    }

    @Test
    fun `config mode cannot be combined with direct event input`() {
        val result = runCli("--config", "agent-desk.config.properties", "--stdin")

        result.exitCode shouldBe 2
        result.error shouldContain "Choose only one input mode."
        assertPublicSafe(result.error)
        result.output shouldBe ""
    }

    @Test
    fun `unknown option fails without echoing the argument`() {
        val result = runCli("--private-token-file=/home/operator/private-token.txt")

        result.exitCode shouldBe 2
        result.error shouldContain "Unknown option."
        assertPublicSafe(result.error)
        result.output shouldBe ""
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
        text shouldNotContain "/home/"
        text shouldNotContain "private-token"
        text.lowercase() shouldNotContain "discord"
        text.lowercase() shouldNotContain "op://"
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

        private const val UNSUPPORTED_EVENT =
            """{"id":"event:agent-task:42:paused","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.paused","payload":{}}"""
    }
}
