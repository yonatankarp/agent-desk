package com.yonatankarp.agentdesk.app.persistence

import com.yonatankarp.agentdesk.app.fixtures.AppFixtures.workStartedEvent
import com.yonatankarp.agentdesk.app.fixtures.auditEntry
import com.yonatankarp.agentdesk.app.serialization.WorkEventJson
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Cross-store coverage for the shared append-only NDJSON mechanics: the
 * single per-path lock map must serialize same-path appends, keep distinct
 * paths independent, and isolate torn-store failures per store.
 */
class SharedNdjsonStoreConcurrencyTest :
    BehaviorSpec({
        fun <T> runConcurrently(tasks: List<() -> T>): List<Result<T>> {
            val executor = Executors.newFixedThreadPool(tasks.size)
            val ready = CountDownLatch(tasks.size)
            val start = CountDownLatch(1)
            val futures = tasks.map { task ->
                executor.submit<Result<T>> {
                    ready.countDown()
                    start.await(5, TimeUnit.SECONDS)
                    runCatching { task() }
                }
            }

            return try {
                ready.await(5, TimeUnit.SECONDS)
                start.countDown()
                futures.map { it.get(10, TimeUnit.SECONDS) }
            } finally {
                executor.shutdownNow()
            }
        }

        given("many repository instances racing on one store path") {
            `when`("each appends a distinct event concurrently") {
                then("every append serializes: all records persist as whole decodable lines") {
                    val storePath = tempStorePath("events.ndjson")
                    val events = List(8) { index ->
                        workStartedEvent(id = WorkEventId.parse("event:agent-task:42:race-$index"))
                    }

                    val outcomes = runConcurrently(
                        events.map { event ->
                            { LocalFileWorkEventRepository(storePath).append(event) }
                        },
                    )

                    outcomes.count { it.isSuccess } shouldBe events.size
                    val result = LocalFileWorkEventRepository(storePath).readAll()
                    result.trailingCorruption shouldBe null
                    result.events.map { it.id } shouldContainExactlyInAnyOrder events.map { it.id }
                    Files.readAllLines(storePath) shouldHaveSize events.size
                }
            }
        }

        given("an event store and an audit store on distinct paths") {
            `when`("both stores are appended to concurrently") {
                then("each append lands in its own store") {
                    val eventPath = tempStorePath("events.ndjson")
                    val auditPath = tempStorePath("audit.ndjson")
                    val event = workStartedEvent()
                    val entry = auditEntry(minute = 10)

                    val outcomes = runConcurrently(
                        listOf(
                            { LocalFileWorkEventRepository(eventPath).append(event) },
                            { LocalFileAuditRecordRepository(auditPath).append(entry) },
                        ),
                    )

                    outcomes.count { it.isSuccess } shouldBe 2
                    LocalFileWorkEventRepository(eventPath).readAll().events.shouldContainExactly(event)
                    LocalFileAuditRecordRepository(auditPath).readAll().entries.shouldContainExactly(entry)
                }
            }
        }

        given("a torn trailing record in the event store") {
            `when`("appends race against a healthy audit store") {
                then("the torn store blocks its own append untouched while the other store stays unaffected") {
                    val eventPath = tempStorePath("events.ndjson")
                    val auditPath = tempStorePath("audit.ndjson")
                    val tornContent = WorkEventJson.encode(workStartedEvent()) + "\n" + "{\"id\":\"event:par"
                    Files.writeString(eventPath, tornContent)
                    val entry = auditEntry(minute = 10)

                    val outcomes = runConcurrently(
                        listOf<() -> Unit>(
                            { LocalFileWorkEventRepository(eventPath).append(workStartedEvent(id = WorkEventId.parse("event:agent-task:42:next"))) },
                            { LocalFileAuditRecordRepository(auditPath).append(entry) },
                        ),
                    )

                    val eventFailure = outcomes[0].exceptionOrNull() as WorkEventStoreException
                    eventFailure.reason shouldBe WorkEventStoreFailure.AppendBlockedByTornRecord(
                        TornTrailingRecord(lineNumber = 2, recoveredEventCount = 1),
                    )
                    eventFailure.message.orEmpty().shouldBePublicSafe()
                    Files.readString(eventPath) shouldBe tornContent
                    outcomes[1].isSuccess shouldBe true
                    LocalFileAuditRecordRepository(auditPath).readAll().entries.shouldContainExactly(entry)
                }
            }
        }

        given("an event store and an audit store misconfigured onto the same path") {
            `when`("both stores append concurrently") {
                then("the shared path lock serializes them: one whole record persists and the other store fails cleanly") {
                    val sharedPath = tempStorePath("shared.ndjson")

                    val outcomes = runConcurrently(
                        listOf<() -> Unit>(
                            { LocalFileWorkEventRepository(sharedPath).append(workStartedEvent()) },
                            { LocalFileAuditRecordRepository(sharedPath).append(auditEntry(minute = 10)) },
                        ),
                    )

                    outcomes.count { it.isSuccess } shouldBe 1
                    val failure = outcomes.single { it.isFailure }.exceptionOrNull()
                    val failureIsForeignRecord = when (failure) {
                        is WorkEventStoreException -> failure.reason == WorkEventStoreFailure.CorruptRecord(lineNumber = 1)
                        is AuditStoreException -> failure.reason == AuditStoreFailure.CorruptRecord(lineNumber = 1)
                        else -> false
                    }
                    failureIsForeignRecord shouldBe true
                    failure?.message.orEmpty().shouldBePublicSafe()
                    Files.readAllLines(sharedPath) shouldHaveSize 1
                }
            }
        }
    })

private fun tempStorePath(fileName: String): Path = Files.createTempDirectory("agent-desk-shared-store-test").resolve(fileName)
