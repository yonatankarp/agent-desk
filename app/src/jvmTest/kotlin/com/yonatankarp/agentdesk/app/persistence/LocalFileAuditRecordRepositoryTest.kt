package com.yonatankarp.agentdesk.app.persistence

import com.yonatankarp.agentdesk.app.fixtures.auditEntry
import com.yonatankarp.agentdesk.app.operator.audit.AuditEntryId
import com.yonatankarp.agentdesk.app.operator.audit.AuditResult
import com.yonatankarp.agentdesk.app.serialization.AuditRecordJson
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class LocalFileAuditRecordRepositoryTest :
    BehaviorSpec({
        given("a missing local audit store") {
            `when`("records are read") {
                then("it returns an empty trail") {
                    LocalFileAuditRecordRepository(tempStorePath()).readAll().entries shouldBe emptyList()
                }
            }
        }

        given("decisions written across every permission outcome") {
            `when`("a fresh repository reconstructs the trail from disk") {
                then("every outcome including the denial survives restart, in append order") {
                    val storePath = tempStorePath()
                    val writer = LocalFileAuditRecordRepository(storePath)
                    val trail = listOf(
                        auditEntry(result = AuditResult.Approved, minute = 10),
                        auditEntry(result = AuditResult.Rejected, minute = 20),
                        auditEntry(result = AuditResult.Canceled, minute = 30),
                        auditEntry(result = AuditResult.RequiresClarification, minute = 40),
                        auditEntry(result = AuditResult.Unsupported, minute = 50),
                    )

                    trail.forEach(writer::append)
                    val reconstructed = LocalFileAuditRecordRepository(storePath).readAll()

                    reconstructed.entries.shouldContainExactly(trail)
                    reconstructed.entries.map { it.result }.shouldContainExactly(
                        AuditResult.Approved,
                        AuditResult.Rejected,
                        AuditResult.Canceled,
                        AuditResult.RequiresClarification,
                        AuditResult.Unsupported,
                    )
                }
            }

            `when`("the raw store bytes are inspected") {
                then("every persisted record is public-safe") {
                    val storePath = tempStorePath()
                    val writer = LocalFileAuditRecordRepository(storePath)
                    writer.append(auditEntry(result = AuditResult.Rejected, minute = 10))
                    writer.append(auditEntry(result = AuditResult.Approved, minute = 20))

                    Files.readString(storePath).shouldBePublicSafe()
                }
            }
        }

        given("idempotency keys derived from canonical timestamps") {
            `when`("the same decision is appended at differing fractional precision") {
                then("the second append is rejected as a duplicate without echoing the id") {
                    val storePath = tempStorePath()
                    val repository = LocalFileAuditRecordRepository(storePath)
                    val verbose = EventTimestamp.parse("2026-06-02T21:10:00.500Z")
                    val canonical = EventTimestamp.parse("2026-06-02T21:10:00.5Z")
                    val first = auditEntry(
                        minute = 10,
                        id = AuditEntryId.parse("audit:agent-task:42:resume:decision:$verbose"),
                    )
                    val second = auditEntry(
                        minute = 20,
                        id = AuditEntryId.parse("audit:agent-task:42:resume:decision:$canonical"),
                    )

                    repository.append(first)
                    val error = shouldThrow<AuditStoreException> {
                        repository.append(second)
                    }

                    assertSoftly(error) {
                        reason shouldBe AuditStoreFailure.DuplicateRecordId()
                        message.orEmpty() shouldContain "Duplicate audit record id"
                        message.orEmpty() shouldNotContain first.id.toString()
                        message.orEmpty() shouldNotContain storePath.toString()
                    }
                }
            }
        }

        given("duplicate audit record ids already in the store") {
            `when`("the trail is read") {
                then("read rejects the store with a line-numbered error that never echoes the id") {
                    val storePath = tempStorePath()
                    val entry = auditEntry(minute = 10)
                    Files.writeString(
                        storePath,
                        AuditRecordJson.encode(entry) + "\n" + AuditRecordJson.encode(entry) + "\n",
                    )

                    val error = shouldThrow<AuditStoreException> {
                        LocalFileAuditRecordRepository(storePath).readAll()
                    }

                    assertSoftly(error) {
                        reason shouldBe AuditStoreFailure.DuplicateRecordId(lineNumber = 2)
                        message.orEmpty() shouldContain "line 2"
                        message.orEmpty() shouldNotContain entry.id.toString()
                        message.orEmpty() shouldNotContain storePath.toString()
                    }
                }
            }
        }

        given("a torn trailing audit record") {
            `when`("the trail is read") {
                then("the committed prefix is recovered with a public-safe warning") {
                    val storePath = tempStorePath()
                    val committed = auditEntry(minute = 10)
                    LocalFileAuditRecordRepository(storePath).append(committed)
                    val torn = AuditRecordJson.encode(auditEntry(minute = 20))
                    Files.writeString(storePath, Files.readString(storePath) + torn.take(torn.length / 2))

                    val result = LocalFileAuditRecordRepository(storePath).readAll()

                    assertSoftly(result) {
                        entries.shouldContainExactly(committed)
                        trailingCorruption shouldBe TornTrailingAuditRecord(
                            lineNumber = 2,
                            recoveredEntryCount = 1,
                        )
                    }
                    result.trailingCorruption?.publicSafeMessage().orEmpty() shouldNotContain storePath.toString()
                }
            }

            `when`("an append is attempted") {
                then("it refuses until the store is repaired and leaves the file untouched") {
                    val storePath = tempStorePath()
                    LocalFileAuditRecordRepository(storePath).append(auditEntry(minute = 10))
                    val torn = AuditRecordJson.encode(auditEntry(minute = 20))
                    val tornContent = Files.readString(storePath) + torn.take(torn.length / 2)
                    Files.writeString(storePath, tornContent)

                    val error = shouldThrow<AuditStoreException> {
                        LocalFileAuditRecordRepository(storePath).append(auditEntry(minute = 30))
                    }

                    error.reason shouldBe AuditStoreFailure.AppendBlockedByTornRecord(
                        TornTrailingAuditRecord(lineNumber = 2, recoveredEntryCount = 1),
                    )
                    Files.readString(storePath) shouldBe tornContent
                }
            }
        }

        given("a corrupt newline-terminated audit record") {
            `when`("the trail is read") {
                then("it fails hard with a line-numbered public-safe error") {
                    val storePath = tempStorePath()
                    LocalFileAuditRecordRepository(storePath).append(auditEntry(minute = 10))
                    Files.writeString(storePath, Files.readString(storePath) + "not-json\n")
                    LocalFileAuditRecordRepository(storePath).let { repository ->
                        val error = shouldThrow<AuditStoreException> {
                            repository.readAll()
                        }

                        assertSoftly(error) {
                            reason shouldBe AuditStoreFailure.CorruptRecord(lineNumber = 2)
                            message.orEmpty() shouldNotContain storePath.toString()
                        }
                    }
                }
            }
        }

        given("an oversized audit store") {
            `when`("the trail is read") {
                then("it rejects the store before loading any bytes") {
                    val storePath = tempStorePath()
                    LocalFileAuditRecordRepository(storePath).append(auditEntry(minute = 10))

                    val error = shouldThrow<AuditStoreException> {
                        LocalFileAuditRecordRepository(storePath, maxStoreSizeBytes = 8).readAll()
                    }

                    error.reason shouldBe AuditStoreFailure.StoreTooLarge
                    error.message.orEmpty() shouldNotContain storePath.toString()
                }
            }

            `when`("the file exceeds the configured size limit") {
                then("the rejection leaks neither the path nor the configured byte counts") {
                    val storePath = tempStorePath()
                    Files.writeString(storePath, "x".repeat(64))

                    val error = shouldThrow<AuditStoreException> {
                        LocalFileAuditRecordRepository(storePath, maxStoreSizeBytes = 32).readAll()
                    }

                    error.reason shouldBe AuditStoreFailure.StoreTooLarge
                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Configured audit store exceeds the maximum readable size")
                        shouldNotContain(storePath.toString())
                        shouldNotContain("32")
                        shouldNotContain("64")
                    }
                }
            }

            `when`("the file is exactly at the size limit") {
                then("the trail reads successfully") {
                    val storePath = tempStorePath()
                    val entry = auditEntry(minute = 10)
                    val content = AuditRecordJson.encode(entry) + "\n"
                    Files.writeString(storePath, content)

                    val repository = LocalFileAuditRecordRepository(
                        storePath,
                        maxStoreSizeBytes = content.toByteArray(StandardCharsets.UTF_8).size.toLong(),
                    )

                    repository.readAll().entries.shouldContainExactly(entry)
                }
            }
        }

        given("a corrupt audit record in the middle of the store") {
            `when`("the trail is read") {
                then("read fails hard at the corrupt line and does not silently recover past it") {
                    val storePath = tempStorePath()
                    Files.writeString(
                        storePath,
                        "not-json\n" + AuditRecordJson.encode(auditEntry(minute = 10)) + "\n",
                    )

                    val error = shouldThrow<AuditStoreException> {
                        LocalFileAuditRecordRepository(storePath).readAll()
                    }

                    assertSoftly(error) {
                        reason shouldBe AuditStoreFailure.CorruptRecord(lineNumber = 1)
                        message.orEmpty() shouldContain "Corrupt audit record at line 1"
                        message.orEmpty() shouldNotContain storePath.toString()
                    }
                }
            }
        }

        given("a valid final audit record without a trailing newline") {
            `when`("another entry is appended") {
                then("the unterminated record is isolated instead of corrupted") {
                    val storePath = tempStorePath()
                    val first = auditEntry(minute = 10)
                    val second = auditEntry(minute = 20)
                    Files.writeString(storePath, AuditRecordJson.encode(first))

                    LocalFileAuditRecordRepository(storePath).append(second)

                    val result = LocalFileAuditRecordRepository(storePath).readAll()
                    result.entries.shouldContainExactly(first, second)
                    result.trailingCorruption shouldBe null
                }
            }
        }

        given("an unwritable audit store target") {
            `when`("an entry is appended") {
                then("it fails with a path-free public-safe error") {
                    val storePath = Files.createTempDirectory("agent-desk-audit-store-test")

                    val error = shouldThrow<AuditStoreException> {
                        LocalFileAuditRecordRepository(storePath).append(auditEntry(minute = 10))
                    }

                    error.reason shouldBe AuditStoreFailure.AppendFailed
                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Unable to append audit record to configured audit store")
                        shouldNotContain(storePath.toString())
                    }
                }
            }
        }

        given("an unreadable audit store target") {
            `when`("the trail is read") {
                then("it fails with a path-free public-safe error") {
                    val storePath = Files.createTempDirectory("agent-desk-audit-store-test")

                    val error = shouldThrow<AuditStoreException> {
                        LocalFileAuditRecordRepository(storePath).readAll()
                    }

                    error.reason shouldBe AuditStoreFailure.Unreadable
                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Unable to read configured audit store")
                        shouldNotContain(storePath.toString())
                    }
                }
            }
        }
    })

private fun tempStorePath(): Path = Files.createTempDirectory("agent-desk-audit-store-test").resolve("audit.ndjson")
