package com.yonatankarp.agentdesk.app.persistence

import com.yonatankarp.agentdesk.app.fixtures.AppFixtures.workBlockedEvent
import com.yonatankarp.agentdesk.app.fixtures.AppFixtures.workStartedEvent
import com.yonatankarp.agentdesk.app.serialization.WorkEventJson
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.charset.StandardCharsets
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

                    repository.readAll().events shouldBe emptyList()
                }
            }
        }

        given("an empty local event store") {
            `when`("events are read") {
                then("it returns an empty stream") {
                    val storePath = tempStorePath()
                    Files.writeString(storePath, "")

                    LocalFileWorkEventRepository(storePath).readAll().events shouldBe emptyList()
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

                    repository.readAll().events.shouldContainExactly(started, blocked)
                }
            }

            `when`("the raw store bytes are inspected") {
                then("every persisted record is public-safe") {
                    val storePath = tempStorePath()
                    val repository = LocalFileWorkEventRepository(storePath)
                    repository.append(workStartedEvent())
                    repository.append(workBlockedEvent())

                    Files.readString(storePath).shouldBePublicSafe()
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

                    LocalFileWorkEventRepository(storePath).readAll().events.shouldContainExactly(started, blocked)
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
                    LocalFileWorkEventRepository(storePath).readAll().events.shouldContainExactly(event)
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

                    firstRepository.readAll().events.shouldHaveSize(0)
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
                then("append refuses with a repair-needed error before adding another record") {
                    val storePath = tempStorePath()
                    val partialRecord = "{\"id\":\"event:partial\""
                    Files.writeString(storePath, partialRecord)

                    val error = shouldThrow<WorkEventStoreException> {
                        LocalFileWorkEventRepository(storePath).append(
                            workStartedEvent(id = WorkEventId.parse("event:agent-task:42:after-partial")),
                        )
                    }

                    error.reason shouldBe WorkEventStoreFailure.AppendBlockedByTornRecord(
                        trailingCorruption = TornTrailingRecord(
                            lineNumber = 1,
                            recoveredEventCount = 0,
                        ),
                    )
                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Torn trailing record at line 1")
                        shouldContain("blocked until the store is repaired")
                        shouldNotContain(storePath.toString())
                    }
                    Files.readString(storePath) shouldBe partialRecord
                }
            }
        }

        given("a torn trailing record in the event store") {
            `when`("the final record is cut mid-write without a newline") {
                then("it recovers the committed prefix and reports the torn line") {
                    val storePath = tempStorePath()
                    val started = workStartedEvent()
                    val blocked = workBlockedEvent()
                    Files.writeString(
                        storePath,
                        WorkEventJson.encode(started) + "\n" +
                            WorkEventJson.encode(blocked) + "\n" +
                            "{\"id\":\"event:agent-task:46:sta",
                    )

                    val result = LocalFileWorkEventRepository(storePath).readAll()

                    result.events.shouldContainExactly(started, blocked)
                    result.trailingCorruption shouldBe TornTrailingRecord(
                        lineNumber = 3,
                        recoveredEventCount = 2,
                    )
                }
            }

            `when`("the torn record ends in a truncated multi-byte character") {
                then("it recovers the committed prefix") {
                    val storePath = tempStorePath()
                    val started = workStartedEvent()
                    val committed = (WorkEventJson.encode(started) + "\n").toByteArray(StandardCharsets.UTF_8)
                    val torn = "{\"title\":\"désync\"".toByteArray(StandardCharsets.UTF_8)
                    Files.write(storePath, committed + torn.copyOfRange(0, torn.size - 2))

                    val result = LocalFileWorkEventRepository(storePath).readAll()

                    result.events.shouldContainExactly(started)
                    result.trailingCorruption shouldBe TornTrailingRecord(
                        lineNumber = 2,
                        recoveredEventCount = 1,
                    )
                }
            }

            `when`("the store holds only a torn record") {
                then("it recovers an empty history and reports the torn line") {
                    val storePath = tempStorePath()
                    Files.writeString(storePath, "{\"id\":\"event:partial\"")

                    val result = LocalFileWorkEventRepository(storePath).readAll()

                    result.events shouldBe emptyList()
                    result.trailingCorruption shouldBe TornTrailingRecord(
                        lineNumber = 1,
                        recoveredEventCount = 0,
                    )
                }
            }

            `when`("the torn-record warning is rendered") {
                then("it stays path-free and public-safe") {
                    val storePath = tempStorePath()
                    Files.writeString(
                        storePath,
                        WorkEventJson.encode(workStartedEvent()) + "\n" + "{\"id\":\"event:par",
                    )

                    val warning = LocalFileWorkEventRepository(storePath).readAll().trailingCorruption

                    assertSoftly(warning?.publicSafeMessage().orEmpty()) {
                        shouldContain("line 2")
                        shouldNotContain(storePath.toString())
                        shouldNotContain("event:par")
                    }
                }
            }
        }

        given("a newline-terminated corrupt record at the end of the store") {
            `when`("events are read") {
                then("read still fails hard because the record write completed") {
                    val storePath = tempStorePath()
                    Files.writeString(
                        storePath,
                        WorkEventJson.encode(workStartedEvent()) + "\n" + "{not-json}\n",
                    )

                    val error = shouldThrow<WorkEventStoreException> {
                        LocalFileWorkEventRepository(storePath).readAll()
                    }

                    error.reason shouldBe WorkEventStoreFailure.CorruptRecord(lineNumber = 2)
                }
            }
        }

        given("a corrupt record in the middle of the store") {
            `when`("events are read") {
                then("read fails hard and does not silently recover past it") {
                    val storePath = tempStorePath()
                    Files.writeString(
                        storePath,
                        "{not-json}\n" + WorkEventJson.encode(workStartedEvent()) + "\n",
                    )

                    val error = shouldThrow<WorkEventStoreException> {
                        LocalFileWorkEventRepository(storePath).readAll()
                    }

                    error.reason shouldBe WorkEventStoreFailure.CorruptRecord(lineNumber = 1)
                }
            }
        }

        given("a valid final record without a trailing newline") {
            `when`("events are read") {
                then("all events are readable with no corruption warning") {
                    val storePath = tempStorePath()
                    val started = workStartedEvent()
                    val blocked = workBlockedEvent()
                    Files.writeString(
                        storePath,
                        WorkEventJson.encode(started) + "\n" + WorkEventJson.encode(blocked),
                    )

                    val result = LocalFileWorkEventRepository(storePath).readAll()

                    result.events.shouldContainExactly(started, blocked)
                    result.trailingCorruption shouldBe null
                }
            }

            `when`("another event is appended") {
                then("the unterminated record is isolated instead of corrupted") {
                    val storePath = tempStorePath()
                    val started = workStartedEvent()
                    val blocked = workBlockedEvent()
                    Files.writeString(storePath, WorkEventJson.encode(started))

                    LocalFileWorkEventRepository(storePath).append(blocked)

                    val result = LocalFileWorkEventRepository(storePath).readAll()
                    result.events.shouldContainExactly(started, blocked)
                    result.trailingCorruption shouldBe null
                }
            }
        }

        given("an oversized event store") {
            `when`("the file exceeds the configured size limit") {
                then("read is rejected with a path-free public-safe error before loading") {
                    val storePath = tempStorePath()
                    Files.writeString(storePath, "x".repeat(64))

                    val error = shouldThrow<WorkEventStoreException> {
                        LocalFileWorkEventRepository(storePath, maxStoreSizeBytes = 32).readAll()
                    }

                    error.reason shouldBe WorkEventStoreFailure.StoreTooLarge
                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Configured event store exceeds the maximum readable size")
                        shouldNotContain(storePath.toString())
                        shouldNotContain("32")
                        shouldNotContain("64")
                    }
                }
            }

            `when`("the file is exactly at the size limit") {
                then("read succeeds") {
                    val storePath = tempStorePath()
                    val started = workStartedEvent()
                    val content = WorkEventJson.encode(started) + "\n"
                    Files.writeString(storePath, content)

                    val repository = LocalFileWorkEventRepository(
                        storePath,
                        maxStoreSizeBytes = content.toByteArray(StandardCharsets.UTF_8).size.toLong(),
                    )

                    repository.readAll().events.shouldContainExactly(started)
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
