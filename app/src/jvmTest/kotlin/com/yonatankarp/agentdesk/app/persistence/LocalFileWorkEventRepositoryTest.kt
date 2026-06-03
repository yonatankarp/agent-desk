package com.yonatankarp.agentdesk.app.persistence

import com.yonatankarp.agentdesk.core.domain.events.EventSource
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.WorkBlockedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.serialization.WorkEventJson
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files
import java.nio.file.Path

class LocalFileWorkEventRepositoryTest :
    BehaviorSpec({
        given("a missing local event store") {
            `when`("events are read") {
                then("it returns an empty stream") {
                    val repository = LocalFileWorkEventRepository(tempStorePath())

                    repository.readAll() shouldBe emptyList()
                }
            }
        }

        given("an empty local event store") {
            `when`("events are read") {
                then("it returns an empty stream") {
                    val storePath = tempStorePath()
                    Files.writeString(storePath, "")

                    LocalFileWorkEventRepository(storePath).readAll() shouldBe emptyList()
                }
            }
        }

        given("a local event store") {
            `when`("events are appended and read back") {
                then("it preserves append order and canonical event records") {
                    val repository = LocalFileWorkEventRepository(tempStorePath())
                    val started = workStartedEvent()
                    val blocked = workBlockedEvent()

                    repository.append(started)
                    repository.append(blocked)

                    repository.readAll().shouldContainExactly(started, blocked)
                }
            }
        }

        given("duplicate event ids") {
            `when`("an event with an existing id is appended") {
                then("it is rejected with a public-safe error") {
                    val storePath = tempStorePath()
                    val repository = LocalFileWorkEventRepository(storePath)
                    val event = workStartedEvent()

                    repository.append(event)
                    val error = shouldThrow<WorkEventStoreException> {
                        repository.append(event)
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Duplicate work event id event:agent-task:42:started")
                        shouldNotContain(storePath.toString())
                    }
                }
            }

            `when`("duplicate ids already exist in the store") {
                then("read rejects the store deterministically") {
                    val storePath = tempStorePath()
                    Files.writeString(
                        storePath,
                        WorkEventJson.encode(workStartedEvent()) + "\n" +
                            WorkEventJson.encode(workStartedEvent()) + "\n",
                    )

                    val error = shouldThrow<WorkEventStoreException> {
                        LocalFileWorkEventRepository(storePath).readAll()
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Duplicate work event id event:agent-task:42:started")
                        shouldContain("line 2")
                        shouldNotContain(storePath.toString())
                    }
                }
            }
        }

        given("a corrupt event store") {
            `when`("a record cannot be decoded") {
                then("it fails with a path-free public-safe error") {
                    val storePath = tempStorePath()
                    Files.writeString(storePath, "{not-json}\n")

                    val error = shouldThrow<WorkEventStoreException> {
                        LocalFileWorkEventRepository(storePath).readAll()
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Corrupt work event record at line 1")
                        shouldNotContain(storePath.toString())
                    }
                }
            }
        }
    }) {
    companion object {
        private val workItemId = WorkItemId.parse("agent-task:42")

        private fun tempStorePath(): Path = Files.createTempDirectory("agent-desk-store-test").resolve("events.ndjson")

        private fun workStartedEvent(): WorkEvent = WorkEvent(
            id = WorkEventId.parse("event:agent-task:42:started"),
            occurredAt = EventTimestamp.parse("2026-06-02T21:00:00Z"),
            source = EventSource.parse("mock-adapter"),
            workItemId = workItemId,
            payload = WorkStartedPayload(
                title = WorkItemTitle.parse("Run public hygiene check"),
                summary = WorkSummary.parse("Agent accepted the task and started local checks."),
            ),
        )

        private fun workBlockedEvent(): WorkEvent = WorkEvent(
            id = WorkEventId.parse("event:agent-task:42:blocked"),
            occurredAt = EventTimestamp.parse("2026-06-02T21:05:00Z"),
            source = EventSource.parse("mock-adapter"),
            workItemId = workItemId,
            payload = WorkBlockedPayload(
                reason = WorkSummary.parse("CI failed on the core test task."),
            ),
        )
    }
}
