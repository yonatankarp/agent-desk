package com.yonatankarp.agentdesk.app.runtime

import com.yonatankarp.agentdesk.app.fixtures.AppFixtures.workStartedEvent
import com.yonatankarp.agentdesk.app.persistence.TornTrailingRecord
import com.yonatankarp.agentdesk.app.persistence.WorkEventReadResult
import com.yonatankarp.agentdesk.app.persistence.WorkEventRepository
import com.yonatankarp.agentdesk.app.persistence.WorkEventStoreException
import com.yonatankarp.agentdesk.app.persistence.WorkEventStoreFailure
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class RuntimeWorkEventImporterTest :
    BehaviorSpec({
        given("a runtime event importer") {
            `when`("mock runtime events are imported into an empty repository") {
                then("it appends canonical events in source order") {
                    val repository = InMemoryWorkEventRepository()

                    val result = RuntimeWorkEventImporter(
                        source = MockRuntimeWorkEventSource(),
                        repository = repository,
                    ).importEvents()

                    result.importedCount shouldBe 6
                    result.skippedDuplicateCount shouldBe 0
                    result.diagnostics.summary().imported shouldBe 6
                    result.diagnostics.summary().skippedDuplicate shouldBe 0
                    result.diagnostics.map { it.kind }.shouldContainExactly(
                        RuntimeWorkEventImportDiagnosticKind.Imported,
                        RuntimeWorkEventImportDiagnosticKind.Imported,
                        RuntimeWorkEventImportDiagnosticKind.Imported,
                        RuntimeWorkEventImportDiagnosticKind.Imported,
                        RuntimeWorkEventImportDiagnosticKind.Imported,
                        RuntimeWorkEventImportDiagnosticKind.Imported,
                    )
                    repository.readAll().events.map { it.id.toString() }.shouldContainExactly(
                        "event:agent-task:42:started",
                        "event:agent-task:44:started",
                        "event:agent-task:44:blocked",
                        "event:agent-task:45:started",
                        "event:agent-task:45:needs-decision",
                        "event:agent-task:42:succeeded",
                    )
                }
            }

            `when`("some source events already exist in the repository") {
                then("it skips duplicates without re-appending them") {
                    val repository = InMemoryWorkEventRepository(
                        initialEvents = listOf(workStartedEvent()),
                    )

                    val result = RuntimeWorkEventImporter(
                        source = MockRuntimeWorkEventSource(),
                        repository = repository,
                    ).importEvents()

                    result.importedCount shouldBe 5
                    result.skippedDuplicateCount shouldBe 1
                    result.diagnostics.summary().imported shouldBe 5
                    result.diagnostics.summary().skippedDuplicate shouldBe 1
                    result.diagnostics.first().kind shouldBe RuntimeWorkEventImportDiagnosticKind.SkippedDuplicate
                    result.diagnostics.first().eventId shouldBe "event:agent-task:42:started"
                    repository.readAll().events.map { it.id.toString() }.shouldContainExactly(
                        "event:agent-task:42:started",
                        "event:agent-task:44:started",
                        "event:agent-task:44:blocked",
                        "event:agent-task:45:started",
                        "event:agent-task:45:needs-decision",
                        "event:agent-task:42:succeeded",
                    )
                }
            }

            `when`("the repository cannot be read") {
                then("it fails with a public-safe import error") {
                    val error = shouldThrow<RuntimeWorkEventImportException> {
                        RuntimeWorkEventImporter(
                            source = MockRuntimeWorkEventSource(),
                            repository = InMemoryWorkEventRepository(readFailure = true),
                        ).importEvents()
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Corrupt work event record at line 1")
                        shouldNotContain("/home/")
                        shouldNotContain("private-token")
                    }
                    error.diagnostics.single().kind shouldBe RuntimeWorkEventImportDiagnosticKind.StoreRejected
                    error.diagnostics.single().message shouldBe "Configured event store could not be read."
                }
            }

            `when`("the store reports a torn trailing record") {
                then("import is refused with a public-safe repair message") {
                    val error = shouldThrow<RuntimeWorkEventImportException> {
                        RuntimeWorkEventImporter(
                            source = MockRuntimeWorkEventSource(),
                            repository = InMemoryWorkEventRepository(
                                trailingCorruption = TornTrailingRecord(
                                    lineNumber = 3,
                                    recoveredEventCount = 2,
                                ),
                            ),
                        ).importEvents()
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Torn trailing record at line 3")
                        shouldNotContain("/home/")
                        shouldNotContain("/Users/")
                    }
                    error.diagnostics.single().kind shouldBe RuntimeWorkEventImportDiagnosticKind.StoreRejected
                    error.diagnostics.single().message shouldBe
                        "Configured event store has a torn trailing record; repair it before importing."
                }
            }

            `when`("the runtime source emits unsafe observations") {
                then("it fails without echoing raw observation text") {
                    val error = shouldThrow<RuntimeWorkEventImportException> {
                        RuntimeWorkEventImporter(
                            source = object : RuntimeWorkEventSource {
                                override fun loadObservations(): List<RuntimeWorkObservation> = listOf(
                                    RuntimeWorkObservation(
                                        eventId = "event:agent-task:99:blocked",
                                        occurredAt = "2026-06-02T21:30:00Z",
                                        source = "mock-adapter",
                                        workItemId = "agent-task:99",
                                        kind = RuntimeWorkObservationKind.Blocked,
                                        reason = "Read failed at ${privateLinuxPath("private-token.txt")}",
                                    ),
                                )
                            },
                            repository = InMemoryWorkEventRepository(),
                        ).importEvents()
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Runtime observations could not be imported.")
                        shouldNotContain("/home/")
                        shouldNotContain("private-token")
                    }
                    error.diagnostics.single().kind shouldBe RuntimeWorkEventImportDiagnosticKind.UnsafeRejected
                }
            }

            `when`("append fails after source loading") {
                then("it reports a path-free store error") {
                    val error = shouldThrow<RuntimeWorkEventImportException> {
                        RuntimeWorkEventImporter(
                            source = object : RuntimeWorkEventSource {
                                override fun loadObservations(): List<RuntimeWorkObservation> = listOf(
                                    RuntimeWorkObservation(
                                        eventId = "event:agent-task:99:started",
                                        occurredAt = "2026-06-02T21:30:00Z",
                                        source = "mock-adapter",
                                        workItemId = "agent-task:99",
                                        kind = RuntimeWorkObservationKind.Started,
                                        title = "Run public hygiene check",
                                    ),
                                )
                            },
                            repository = InMemoryWorkEventRepository(appendFailure = true),
                        ).importEvents()
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Configured event store contains a duplicate work event id at line 12.")
                        shouldNotContain("event:agent-task:99:started")
                    }
                    error.diagnostics.single().kind shouldBe RuntimeWorkEventImportDiagnosticKind.StoreRejected
                    error.diagnostics.single().message shouldBe "Configured event store rejected a runtime event."
                }
            }
        }
    })

private class InMemoryWorkEventRepository(
    initialEvents: List<WorkEvent> = emptyList(),
    private val readFailure: Boolean = false,
    private val appendFailure: Boolean = false,
    private val trailingCorruption: TornTrailingRecord? = null,
) : WorkEventRepository {
    private val events = initialEvents.toMutableList()

    override fun append(event: WorkEvent) {
        if (appendFailure) {
            throw WorkEventStoreException(
                WorkEventStoreFailure.DuplicateEventId(
                    eventId = event.id.toString(),
                    lineNumber = 12,
                ),
            )
        }
        events.add(event)
    }

    override fun readAll(): WorkEventReadResult {
        if (readFailure) {
            throw WorkEventStoreException(WorkEventStoreFailure.CorruptRecord(lineNumber = 1))
        }
        return WorkEventReadResult(events = events.toList(), trailingCorruption = trailingCorruption)
    }
}

private fun privateLinuxPath(fileName: String): String = "/home/" + "operator/$fileName"
