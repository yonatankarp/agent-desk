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

class OpenClawRuntimeObservationFileSourceTest :
    BehaviorSpec({
        given("a sanitized OpenClaw observation export") {
            `when`("the export contains public-safe observations") {
                then("the file source loads runtime observations without private runtime access") {
                    val exportPath = tempExport(
                        """
                        {
                          "schemaVersion": 1,
                          "observations": [
                            {
                              "eventId": "event:agent-task:210:started",
                              "occurredAt": "2026-06-05T18:40:00Z",
                              "source": "openclaw-local",
                              "workItemId": "agent-task:210",
                              "kind": "started",
                              "title": "Implement sanitized adapter",
                              "summary": "Agent started the approved runtime adapter slice.",
                              "evidenceReferences": [
                                {
                                  "kind": "sanitized-note",
                                  "label": "Adapter decision",
                                  "target": "docs/runtime-adapter-scope-decision.md"
                                }
                              ]
                            },
                            {
                              "eventId": "event:agent-task:210:blocked",
                              "occurredAt": "2026-06-05T18:45:00Z",
                              "source": "openclaw-local",
                              "workItemId": "agent-task:210",
                              "kind": "blocked",
                              "reason": "Waiting for CI evidence."
                            }
                          ]
                        }
                        """.trimIndent(),
                    )

                    val observations = OpenClawRuntimeObservationFileSource(exportPath).loadObservations()

                    observations.map { it.kind }.shouldContainExactly(
                        RuntimeWorkObservationKind.Started,
                        RuntimeWorkObservationKind.Blocked,
                    )
                    assertSoftly(observations.first()) {
                        eventId shouldBe "event:agent-task:210:started"
                        source shouldBe "openclaw-local"
                        workItemId shouldBe "agent-task:210"
                        title shouldBe "Implement sanitized adapter"
                        summary shouldBe "Agent started the approved runtime adapter slice."
                        evidenceReferences.single().kind shouldBe "sanitized-note"
                        evidenceReferences.single().label shouldBe "Adapter decision"
                        evidenceReferences.single().target shouldBe "docs/runtime-adapter-scope-decision.md"
                    }
                }
            }

            `when`("each accepted lifecycle kind is present") {
                then("the file source maps every wire kind") {
                    val exportPath = tempExport(
                        """
                        {
                          "schemaVersion": 1,
                          "observations": [
                            {
                              "eventId": "event:agent-task:211:started",
                              "occurredAt": "2026-06-05T18:40:00Z",
                              "source": "openclaw-local",
                              "workItemId": "agent-task:211",
                              "kind": "started",
                              "title": "Add fixture"
                            },
                            {
                              "eventId": "event:agent-task:211:needs-decision",
                              "occurredAt": "2026-06-05T18:41:00Z",
                              "source": "openclaw-local",
                              "workItemId": "agent-task:211",
                              "kind": "needs-decision",
                              "reason": "Operator should choose fixture order."
                            },
                            {
                              "eventId": "event:agent-task:211:blocked",
                              "occurredAt": "2026-06-05T18:42:00Z",
                              "source": "openclaw-local",
                              "workItemId": "agent-task:211",
                              "kind": "blocked",
                              "reason": "Fixture review is pending."
                            },
                            {
                              "eventId": "event:agent-task:211:succeeded",
                              "occurredAt": "2026-06-05T18:43:00Z",
                              "source": "openclaw-local",
                              "workItemId": "agent-task:211",
                              "kind": "succeeded"
                            },
                            {
                              "eventId": "event:agent-task:211:failed",
                              "occurredAt": "2026-06-05T18:44:00Z",
                              "source": "openclaw-local",
                              "workItemId": "agent-task:211",
                              "kind": "failed",
                              "reason": "Fixture validation failed."
                            },
                            {
                              "eventId": "event:agent-task:211:canceled",
                              "occurredAt": "2026-06-05T18:45:00Z",
                              "source": "openclaw-local",
                              "workItemId": "agent-task:211",
                              "kind": "canceled",
                              "reason": "Operator canceled the slice."
                            }
                          ]
                        }
                        """.trimIndent(),
                    )

                    OpenClawRuntimeObservationFileSource(exportPath)
                        .loadObservations()
                        .map { it.kind }
                        .shouldContainExactly(
                            RuntimeWorkObservationKind.Started,
                            RuntimeWorkObservationKind.NeedsDecision,
                            RuntimeWorkObservationKind.Blocked,
                            RuntimeWorkObservationKind.Succeeded,
                            RuntimeWorkObservationKind.Failed,
                            RuntimeWorkObservationKind.Canceled,
                        )
                }
            }

            `when`("the export contains unsupported or extra data") {
                then("it fails with a public-safe message") {
                    val exportPath = tempExport(
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
                              "title": "Run smoke command",
                              "rawTranscript": "private transcript text"
                            }
                          ]
                        }
                        """.trimIndent(),
                    )

                    val error = shouldThrow<OpenClawRuntimeObservationFileSourceException> {
                        OpenClawRuntimeObservationFileSource(exportPath).loadObservations()
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Sanitized observation export is invalid.")
                        shouldNotContain("rawTranscript")
                        shouldNotContain("private transcript")
                        shouldNotContain(exportPath.toString())
                    }
                }
            }

            `when`("the export contains raw private-looking identifiers") {
                then("the importer rejects them through the existing public-safe mapper") {
                    val rawIdentifier = "123456789" + "012345678"
                    val exportPath = tempExport(
                        """
                        {
                          "schemaVersion": 1,
                          "observations": [
                            {
                              "eventId": "event:agent-task:213:started",
                              "occurredAt": "2026-06-05T18:40:00Z",
                              "source": "openclaw-local",
                              "workItemId": "agent-task:$rawIdentifier",
                              "kind": "started",
                              "title": "Import private id"
                            }
                          ]
                        }
                        """.trimIndent(),
                    )
                    val source = OpenClawRuntimeObservationFileSource(exportPath)

                    val error = shouldThrow<RuntimeWorkEventImportException> {
                        RuntimeWorkEventImporter(
                            source = source,
                            repository = OpenClawFileSourceTestRepository(),
                        ).importEvents()
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Runtime observations could not be imported.")
                        shouldNotContain(rawIdentifier)
                        shouldNotContain(exportPath.toString())
                    }
                }
            }
        }
    })

private fun tempExport(content: String): Path {
    val path = Files.createTempFile("agent-desk-sanitized-observations", ".json")
    Files.writeString(path, content)
    return path
}

private class OpenClawFileSourceTestRepository : WorkEventRepository {
    private val events = mutableListOf<WorkEvent>()

    override fun append(event: WorkEvent) {
        events.add(event)
    }

    override fun readAll(): List<WorkEvent> = events.toList()
}
