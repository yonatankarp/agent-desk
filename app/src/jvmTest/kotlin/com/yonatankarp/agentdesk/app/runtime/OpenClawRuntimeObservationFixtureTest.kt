package com.yonatankarp.agentdesk.app.runtime

import com.yonatankarp.agentdesk.app.persistence.WorkEventRepository
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.toPath

class OpenClawRuntimeObservationFixtureTest :
    BehaviorSpec({
        given("the checked-in sanitized OpenClaw export fixture") {
            `when`("the fixture is imported") {
                then("it produces canonical work events in source order") {
                    val repository = FixtureImportRepository()

                    val result = RuntimeWorkEventImporter(
                        source = OpenClawRuntimeObservationFileSource(fixturePath()),
                        repository = repository,
                    ).importEvents()

                    result.importedCount shouldBe 6
                    result.skippedDuplicateCount shouldBe 0
                    repository.readAll().map { it.type.wireName }.shouldContainExactly(
                        "work.started",
                        "work.needs-decision",
                        "work.blocked",
                        "work.succeeded",
                        "work.failed",
                        "work.canceled",
                    )
                    assertSoftly(repository.readAll().first()) {
                        id.toString() shouldBe "event:agent-task:211:started"
                        source.toString() shouldBe "openclaw-local"
                        workItemId.toString() shouldBe "agent-task:211"
                        evidenceReferences.single().kind.wireName shouldBe "sanitized-note"
                        evidenceReferences.single().label.toString() shouldBe "Runtime adapter decision"
                        evidenceReferences.single().target.toString() shouldBe
                            "docs/runtime-adapter-scope-decision.md"
                    }
                }
            }

            `when`("the fixture text is inspected") {
                then("it stays public-safe") {
                    val fixtureText = Files.readString(fixturePath())

                    assertSoftly(fixtureText) {
                        shouldContain("openclaw-local")
                        shouldNotContain("/home/")
                        shouldNotContain("/Users/")
                        shouldNotContain("auth_token")
                        shouldNotContain("bearer ")
                        shouldNotContain("channel:")
                        shouldNotContain("message:")
                        shouldNotContain("raw transcript")
                        shouldNotContain("session:")
                        shouldNotContain("thread:")
                        shouldNotContain("op://")
                    }
                }
            }
        }

        given("a sanitized OpenClaw export with an invalid alias") {
            `when`("the export is imported") {
                then("it fails without echoing the raw private-looking identifier") {
                    val rawIdentifier = "987654321" + "098765432"
                    val exportPath = fixtureTempExport(
                        """
                        {
                          "schemaVersion": 1,
                          "observations": [
                            {
                              "eventId": "event:agent-task:211:started",
                              "occurredAt": "2026-06-05T18:40:00Z",
                              "source": "openclaw-local",
                              "workItemId": "agent-task:$rawIdentifier",
                              "kind": "started",
                              "title": "Import invalid alias"
                            }
                          ]
                        }
                        """.trimIndent(),
                    )

                    val error = shouldThrow<RuntimeWorkEventImportException> {
                        RuntimeWorkEventImporter(
                            source = OpenClawRuntimeObservationFileSource(exportPath),
                            repository = FixtureImportRepository(),
                        ).importEvents()
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Runtime observations could not be imported.")
                        shouldNotContain(rawIdentifier)
                    }
                }
            }
        }
    })

private fun fixturePath(): Path = requireNotNull(
    OpenClawRuntimeObservationFixtureTest::class.java.classLoader
        .getResource("openclaw/sanitized-observations.json"),
) {
    "sanitized OpenClaw fixture resource is missing"
}.toURI().toPath()

private fun fixtureTempExport(content: String): Path {
    val path = Files.createTempFile("agent-desk-invalid-sanitized-observations", ".json")
    Files.writeString(path, content)
    return path
}

private class FixtureImportRepository : WorkEventRepository {
    private val events = mutableListOf<WorkEvent>()

    override fun append(event: WorkEvent) {
        events.add(event)
    }

    override fun readAll(): List<WorkEvent> = events.toList()
}
