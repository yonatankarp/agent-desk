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

class AgentDeskCliActTest :
    BehaviorSpec({
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
    })
