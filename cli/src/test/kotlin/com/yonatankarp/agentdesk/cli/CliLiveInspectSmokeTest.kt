package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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
        }
    })
