package com.yonatankarp.agentdesk.app.persistence

import com.yonatankarp.agentdesk.app.fixtures.AppFixtures.workBlockedEvent
import com.yonatankarp.agentdesk.app.fixtures.AppFixtures.workStartedEvent
import com.yonatankarp.agentdesk.app.serialization.WorkEventJson
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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

            `when`("two repository instances append unique events") {
                then("both events are preserved in store order") {
                    val storePath = tempStorePath()
                    val firstRepository = LocalFileWorkEventRepository(storePath)
                    val secondRepository = LocalFileWorkEventRepository(storePath)
                    val started = workStartedEvent()
                    val blocked = workBlockedEvent()

                    firstRepository.append(started)
                    secondRepository.append(blocked)

                    LocalFileWorkEventRepository(storePath).readAll().shouldContainExactly(started, blocked)
                }
            }

            `when`("repository instances append the same event concurrently") {
                then("one append is accepted and the duplicate is rejected") {
                    val storePath = tempStorePath()
                    val event = workStartedEvent()
                    val executor = Executors.newFixedThreadPool(2)
                    val ready = CountDownLatch(2)
                    val start = CountDownLatch(1)
                    val results = List(2) {
                        executor.submit<Result<Unit>> {
                            ready.countDown()
                            start.await(5, TimeUnit.SECONDS)
                            runCatching {
                                LocalFileWorkEventRepository(storePath).append(event)
                            }
                        }
                    }

                    val outcomes = try {
                        ready.await(5, TimeUnit.SECONDS)
                        start.countDown()
                        results.map { it.get(5, TimeUnit.SECONDS) }
                    } finally {
                        executor.shutdownNow()
                    }

                    outcomes.count { it.isSuccess } shouldBe 1
                    outcomes.count {
                        it.exceptionOrNull()
                            ?.message
                            ?.contains("Duplicate work event id event:agent-task:42:started") == true
                    } shouldBe 1
                    LocalFileWorkEventRepository(storePath).readAll().shouldContainExactly(event)
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

                    error.reason shouldBe WorkEventStoreFailure.DuplicateEventId(
                        eventId = "event:agent-task:42:started",
                    )
                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Duplicate work event id event:agent-task:42:started")
                        shouldNotContain(storePath.toString())
                    }
                }
            }

            `when`("another repository instance appends a duplicate after ids were read") {
                then("append re-checks the current store and rejects the stale duplicate") {
                    val storePath = tempStorePath()
                    val firstRepository = LocalFileWorkEventRepository(storePath)
                    val secondRepository = LocalFileWorkEventRepository(storePath)
                    val event = workStartedEvent()

                    firstRepository.readAll().shouldHaveSize(0)
                    secondRepository.append(event)
                    val error = shouldThrow<WorkEventStoreException> {
                        firstRepository.append(event)
                    }

                    error.reason shouldBe WorkEventStoreFailure.DuplicateEventId(
                        eventId = "event:agent-task:42:started",
                    )
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

                    error.reason shouldBe WorkEventStoreFailure.DuplicateEventId(
                        eventId = "event:agent-task:42:started",
                        lineNumber = 2,
                    )
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

                    error.reason shouldBe WorkEventStoreFailure.CorruptRecord(lineNumber = 1)
                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Corrupt work event record at line 1")
                        shouldNotContain(storePath.toString())
                    }
                }
            }

            `when`("a partial record exists before append") {
                then("append fails before adding another record") {
                    val storePath = tempStorePath()
                    val partialRecord = "{\"id\":\"event:partial\""
                    Files.writeString(storePath, partialRecord)

                    val error = shouldThrow<WorkEventStoreException> {
                        LocalFileWorkEventRepository(storePath).append(
                            workStartedEvent(id = WorkEventId.parse("event:agent-task:42:after-partial")),
                        )
                    }

                    error.reason shouldBe WorkEventStoreFailure.CorruptRecord(lineNumber = 1)
                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Corrupt work event record at line 1")
                        shouldNotContain(storePath.toString())
                    }
                    Files.readString(storePath) shouldBe partialRecord
                }
            }
        }

        given("an unwritable event store target") {
            `when`("an event is appended") {
                then("it fails with a path-free public-safe error") {
                    val storePath = Files.createTempDirectory("agent-desk-store-test")

                    val error = shouldThrow<WorkEventStoreException> {
                        LocalFileWorkEventRepository(storePath).append(workStartedEvent())
                    }

                    error.reason shouldBe WorkEventStoreFailure.AppendFailed
                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Unable to append work event to configured event store")
                        shouldNotContain(storePath.toString())
                    }
                }
            }
        }

        given("an unreadable event store target") {
            `when`("events are read") {
                then("it fails with a path-free public-safe error") {
                    val storePath = Files.createTempDirectory("agent-desk-store-test")

                    val error = shouldThrow<WorkEventStoreException> {
                        LocalFileWorkEventRepository(storePath).readAll()
                    }

                    error.reason shouldBe WorkEventStoreFailure.Unreadable
                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Unable to read configured event store")
                        shouldNotContain(storePath.toString())
                    }
                }
            }
        }
    }) {
    companion object {
        private fun tempStorePath(): Path = Files.createTempDirectory("agent-desk-store-test").resolve("events.ndjson")
    }
}
