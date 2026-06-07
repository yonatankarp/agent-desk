package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files
import kotlin.io.path.readLines

class CliImportTest :
    BehaviorSpec({
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
    })
