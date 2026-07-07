package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.app.serialization.WorkEventJson
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import com.yonatankarp.agentdesk.testfixtures.workEvents
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files
import kotlin.io.path.readLines

class CliLiveInspectSmokeTest :
    BehaviorSpec({
        given("the synthetic live inspect smoke command") {
            `when`("approval is missing") {
                then("it denies before the synthetic adapter is called and writes audit evidence") {
                    val eventFile = Files.createTempFile("agent-desk-live-inspect-events", ".ndjson")
                    val auditFile = Files.createTempFile("agent-desk-live-inspect-audit", ".ndjson")
                    Files.writeString(eventFile, "$STARTED_EVENT\n$NEEDS_DECISION_EVENT\n")

                    val result = runCli(
                        "live-inspect-smoke",
                        "agent-task:42",
                        "--event-store",
                        eventFile.toString(),
                        "--audit-store",
                        auditFile.toString(),
                    )

                    result.exitCode shouldBe 3
                    result.output shouldContain "Live inspect proposal"
                    result.output shouldContain "- Denied"
                    result.output shouldContain "Inspect proposal denied by operator."
                    result.output.shouldBePublicSafe()
                    result.error shouldBe ""
                    auditFile.readLines().let { records ->
                        records shouldHaveSize 2
                        records.joinToString("\n") shouldContain "\"action\":\"live-inspect.approval.denied\""
                    }
                }
            }

            `when`("approval is present") {
                then("it executes only the synthetic adapter and renders public-safe audit evidence") {
                    val eventFile = Files.createTempFile("agent-desk-live-inspect-events", ".ndjson")
                    val auditFile = Files.createTempFile("agent-desk-live-inspect-audit", ".ndjson")
                    Files.writeString(eventFile, "$STARTED_EVENT\n$NEEDS_DECISION_EVENT\n")

                    val result = runCli(
                        "live-inspect-smoke",
                        "agent-task:42",
                        "--event-store",
                        eventFile.toString(),
                        "--audit-store",
                        auditFile.toString(),
                        "--approve",
                    )

                    result.exitCode shouldBe 0
                    result.output shouldContain "- Approved"
                    result.output shouldContain "Synthetic inspect completed for agent-task:42 on host:primary."
                    result.output shouldContain "live-inspect.adapter.succeeded"
                    result.output.shouldBePublicSafe()
                    result.error shouldBe ""
                    auditFile.readLines().let { records ->
                        records shouldHaveSize 5
                        records.joinToString("\n") shouldContain "\"action\":\"live-inspect.output.rendered\""
                        records.joinToString("\n").shouldBePublicSafe()
                    }
                }
            }

            `when`("approval does not match the exact proposal") {
                then("it fails closed and records mismatch audit evidence") {
                    val eventFile = Files.createTempFile("agent-desk-live-inspect-events", ".ndjson")
                    val auditFile = Files.createTempFile("agent-desk-live-inspect-audit", ".ndjson")
                    Files.writeString(
                        eventFile,
                        workEvents {
                            started()
                            needsDecision()
                        }.joinToString(separator = "\n", postfix = "\n", transform = WorkEventJson::encode),
                    )

                    val result = LiveInspectSmokeCommand.execute(
                        workItemId = WorkItemId.parse("agent-task:42"),
                        eventStorePath = eventFile.toString(),
                        auditStorePath = auditFile.toString(),
                        approvalMode = LiveInspectSmokeCommand.ApprovalMode.Mismatched,
                        now = EventTimestamp.parse("2026-06-02T21:25:00Z"),
                    )

                    result.exitCode shouldBe 3
                    result.text shouldContain "Live inspect proposal"
                    result.text shouldContain "- Denied"
                    result.text shouldContain "Inspect approval did not match the exact proposal."
                    result.text shouldNotContain "Synthetic inspect completed"
                    result.text.shouldBePublicSafe()
                    auditFile.readLines().joinToString("\n").let { records ->
                        records shouldContain "\"action\":\"live-inspect.approval.mismatched\""
                        records shouldNotContain "\"action\":\"live-inspect.adapter.started\""
                        records.shouldBePublicSafe()
                    }
                }
            }
        }
    })
