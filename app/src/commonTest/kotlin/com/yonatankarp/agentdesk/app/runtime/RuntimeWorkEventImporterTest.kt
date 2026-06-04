package com.yonatankarp.agentdesk.app.runtime

import com.yonatankarp.agentdesk.app.fixtures.AppFixtures.workStartedEvent
import com.yonatankarp.agentdesk.app.persistence.WorkEventRepository
import com.yonatankarp.agentdesk.app.persistence.WorkEventStoreException
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
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
                    repository.readAll().map { it.id.toString() }.shouldContainExactly(
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
                    repository.readAll().map { it.id.toString() }.shouldContainExactly(
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
                }
            }

            `when`("the runtime source emits unsafe observations") {
                then("it fails without echoing raw observation text") {
                    val error = shouldThrow<RuntimeWorkEventImportException> {
                        RuntimeWorkEventImporter(
                            source = object : RuntimeWorkEventSource {
                                override fun loadEvents(): List<WorkEvent> {
                                    SanitizedRuntimeObservationMapper().toWorkEvent(
                                        RuntimeWorkObservation(
                                            eventId = "event:agent-task:99:blocked",
                                            occurredAt = "2026-06-02T21:30:00Z",
                                            source = "mock-adapter",
                                            workItemId = "agent-task:99",
                                            kind = RuntimeWorkObservationKind.Blocked,
                                            reason = "Read failed at /home/operator/private-token.txt",
                                        ),
                                    )
                                    return emptyList()
                                }
                            },
                            repository = InMemoryWorkEventRepository(),
                        ).importEvents()
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Runtime observations could not be imported.")
                        shouldNotContain("/home/")
                        shouldNotContain("private-token")
                    }
                }
            }

            `when`("append fails after source loading") {
                then("it reports a path-free store error") {
                    val error = shouldThrow<RuntimeWorkEventImportException> {
                        RuntimeWorkEventImporter(
                            source = object : RuntimeWorkEventSource {
                                override fun loadEvents(): List<WorkEvent> = listOf(
                                    workStartedEvent(
                                        id = WorkEventId.parse("event:private-token:started"),
                                    ),
                                )
                            },
                            repository = InMemoryWorkEventRepository(appendFailure = true),
                        ).importEvents()
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Configured event store contains a duplicate work event id.")
                        shouldNotContain("private-token")
                    }
                }
            }
        }
    })

private class InMemoryWorkEventRepository(
    initialEvents: List<WorkEvent> = emptyList(),
    private val readFailure: Boolean = false,
    private val appendFailure: Boolean = false,
) : WorkEventRepository {
    private val events = initialEvents.toMutableList()

    override fun append(event: WorkEvent) {
        if (appendFailure) {
            throw WorkEventStoreException("Duplicate work event id ${event.id} in configured event store")
        }
        events.add(event)
    }

    override fun readAll(): List<WorkEvent> {
        if (readFailure) {
            throw WorkEventStoreException("Corrupt work event record at line 1 in configured event store")
        }
        return events.toList()
    }
}
