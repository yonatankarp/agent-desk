package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readBytes

class CliReportTest :
    BehaviorSpec({
        fun seededEventFile(): Path {
            val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")
            Files.writeString(eventFile, "$STARTED_EVENT\n$NEEDS_DECISION_EVENT\n")
            return eventFile
        }

        fun seedAuditStoreViaAct(
            eventFile: Path,
            approve: Boolean,
        ): Path {
            val auditFile = Files.createTempFile("agent-desk-cli-audit", ".ndjson")
            val args = buildList {
                addAll(listOf("act", "resume", "agent-task:42"))
                addAll(listOf("--event-store", eventFile.toString()))
                addAll(listOf("--audit-store", auditFile.toString()))
                if (approve) {
                    add("--approve")
                }
            }
            runCli(*args.toTypedArray())
            return auditFile
        }

        given("the report command") {
            `when`("a work item is reported from a seeded event store without an audit store") {
                then("readiness renders the honest empty projection and the audit section says no store is configured") {
                    val eventFile = seededEventFile()

                    val result = runCli("report", "agent-task:42", "--events", eventFile.toString())

                    result.exitCode shouldBe 0
                    result.output shouldContain "Work item"
                    result.output shouldContain "- agent-task:42 (Needs decision)"
                    result.output shouldContain "Readiness"
                    result.output shouldContain "- Unknown"
                    result.output shouldContain "- Verification was not attempted."
                    result.output shouldContain "- No verification results were recorded."
                    result.output shouldContain "Verification"
                    result.output shouldContain "- none"
                    result.output shouldContain "Audit trail"
                    result.output shouldContain "- No audit store configured. Pass --audit-store <file> to read recorded decisions."
                    result.output.shouldBePublicSafe()
                    result.error shouldBe ""
                }
            }

            `when`("an audit store seeded by an approved act run is reported") {
                then("the trail renders gate, loop, and action records grouped by correlation id with humanized results") {
                    val eventFile = seededEventFile()
                    val auditFile = seedAuditStoreViaAct(eventFile, approve = true)
                    val eventBytesBefore = eventFile.readBytes()
                    val auditBytesBefore = auditFile.readBytes()

                    val result = runCli(
                        "report",
                        "agent-task:42",
                        "--events",
                        eventFile.toString(),
                        "--audit-store",
                        auditFile.toString(),
                    )

                    result.exitCode shouldBe 0
                    result.output shouldContain "Audit trail (3 durable record(s))"
                    result.output shouldContain "- correlation:agent-task:42:"
                    result.output shouldContain "permission.localwrite"
                    result.output shouldContain "mock.resume"
                    result.output shouldContain "Partial success"
                    result.output shouldNotContain "PartialSuccess"
                    result.output.shouldBePublicSafe()
                    result.error shouldBe ""
                    eventFile.readBytes() shouldBe eventBytesBefore
                    auditFile.readBytes() shouldBe auditBytesBefore
                }
            }

            `when`("the audit store holds only a denial record for the work item") {
                then("the partial correlation group renders without fabricating missing records") {
                    val eventFile = seededEventFile()
                    val auditFile = seedAuditStoreViaAct(eventFile, approve = false)

                    val result = runCli(
                        "report",
                        "agent-task:42",
                        "--events",
                        eventFile.toString(),
                        "--audit-store",
                        auditFile.toString(),
                    )

                    result.exitCode shouldBe 0
                    result.output shouldContain "Audit trail (1 durable record(s))"
                    result.output shouldContain "permission.localwrite"
                    result.output shouldContain "Rejected"
                    result.output shouldNotContain "mock.resume"
                    result.output.shouldBePublicSafe()
                }
            }

            `when`("the audit store exists but holds no records for the work item") {
                then("the empty trail is explicit, not silent") {
                    val eventFile = seededEventFile()
                    val auditFile = Files.createTempFile("agent-desk-cli-audit", ".ndjson")

                    val result = runCli(
                        "report",
                        "agent-task:42",
                        "--events",
                        eventFile.toString(),
                        "--audit-store",
                        auditFile.toString(),
                    )

                    result.exitCode shouldBe 0
                    result.output shouldContain "Audit trail (0 durable record(s))"
                    result.output shouldContain "- No durable audit records for this work item."
                    result.output.shouldBePublicSafe()
                }
            }

            `when`("the audit store has a torn trailing record") {
                then("committed records render and the torn record surfaces as a public-safe warning") {
                    val eventFile = seededEventFile()
                    val auditFile = seedAuditStoreViaAct(eventFile, approve = true)
                    Files.writeString(
                        auditFile,
                        Files.readString(auditFile) + """{"id":"audit:agent-task:42:torn""",
                    )

                    val result = runCli(
                        "report",
                        "agent-task:42",
                        "--events",
                        eventFile.toString(),
                        "--audit-store",
                        auditFile.toString(),
                    )

                    result.exitCode shouldBe 0
                    result.output shouldContain "Audit trail (3 durable record(s))"
                    result.error shouldContain "Torn trailing record at line 4 in configured audit store"
                    result.error.shouldBePublicSafe()
                    result.output.shouldBePublicSafe()
                }
            }

            `when`("the audit store has a corrupt committed record") {
                then("the read fails with the public-safe store message") {
                    val eventFile = seededEventFile()
                    val auditFile = Files.createTempFile("agent-desk-cli-audit", ".ndjson")
                    Files.writeString(auditFile, "not-a-valid-audit-record\n")

                    val result = runCli(
                        "report",
                        "agent-task:42",
                        "--events",
                        eventFile.toString(),
                        "--audit-store",
                        auditFile.toString(),
                    )

                    result.exitCode shouldBe 1
                    result.error shouldContain "Corrupt audit record at line 1 in configured audit store"
                    result.error.shouldBePublicSafe()
                    result.output shouldBe ""
                }
            }

            `when`("an unsafe audit store location is requested") {
                then("the report rejects the location with a public-safe error") {
                    val eventFile = seededEventFile()

                    val result = runCli(
                        "report",
                        "agent-task:42",
                        "--events",
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

            `when`("the work item id is unknown") {
                then("the report fails with the shared not-found message") {
                    val eventFile = seededEventFile()

                    val result = runCli("report", "agent-task:99", "--events", eventFile.toString())

                    result.exitCode shouldBe 1
                    result.error shouldContain "Work item was not found."
                    result.error.shouldBePublicSafe()
                    result.output shouldBe ""
                }
            }

            `when`("the event store file is empty") {
                then("the shared empty-input message is rendered") {
                    val eventFile = Files.createTempFile("agent-desk-cli-events", ".ndjson")

                    val result = runCli("report", "agent-task:42", "--events", eventFile.toString())

                    result.exitCode shouldBe 1
                    result.error shouldContain "No event input provided."
                    result.error.shouldBePublicSafe()
                }
            }
        }
    })
