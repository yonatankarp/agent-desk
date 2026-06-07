package com.yonatankarp.agentdesk.app.persistence

import com.yonatankarp.agentdesk.app.operator.audit.AuditEntry
import com.yonatankarp.agentdesk.app.serialization.AuditRecordJson
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.OverlappingFileLockException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * JVM-local audit record store. Mirrors the event-store mechanics: appends
 * serialize through a process-local path lock and a cooperative file lock,
 * then re-read the store before writing.
 */
class LocalFileAuditRecordRepository(
    private val storePath: Path,
    private val maxStoreSizeBytes: Long = DEFAULT_MAX_STORE_SIZE_BYTES,
) : AuditRecordRepository {
    override fun append(entry: AuditEntry) {
        try {
            storePath.parent?.let(Files::createDirectories)
        } catch (error: IOException) {
            throw AuditStoreException(AuditStoreFailure.AppendFailed, error)
        }

        pathLockFor(storePath).withLock {
            try {
                FileChannel.open(
                    storePath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                ).use { channel ->
                    channel.lock().use {
                        val snapshot = readSnapshot()
                        snapshot.trailingCorruption?.let { corruption ->
                            throw AuditStoreException(
                                AuditStoreFailure.AppendBlockedByTornRecord(corruption),
                            )
                        }
                        if (entry.id in snapshot.entryIds) {
                            throw AuditStoreException(AuditStoreFailure.DuplicateRecordId())
                        }

                        channel.position(channel.size())
                        val isolatingNewline = if (snapshot.endsWithoutNewline) "\n" else ""
                        val record = ByteBuffer.wrap(
                            (isolatingNewline + AuditRecordJson.encode(entry) + "\n").toByteArray(StandardCharsets.UTF_8),
                        )
                        while (record.hasRemaining()) {
                            channel.write(record)
                        }
                        channel.force(false)
                    }
                }
            } catch (error: IOException) {
                throw AuditStoreException(AuditStoreFailure.AppendFailed, error)
            } catch (error: OverlappingFileLockException) {
                throw AuditStoreException(AuditStoreFailure.AppendFailed, error)
            }
        }
    }

    override fun readAll(): AuditRecordReadResult = readSnapshot().let {
        AuditRecordReadResult(entries = it.entries, trailingCorruption = it.trailingCorruption)
    }

    private fun readSnapshot(): AuditStoreSnapshot {
        if (!Files.exists(storePath)) {
            return AuditStoreSnapshot(entries = emptyList(), entryIds = mutableSetOf())
        }

        val text = readText()
        val endsWithoutNewline = text.isNotEmpty() && !text.endsWith("\n")
        val lines = text.split("\n")

        val seenIds = mutableSetOf<String>()
        val entries = mutableListOf<AuditEntry>()
        var trailingCorruption: TornTrailingAuditRecord? = null
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                return@forEachIndexed
            }

            val lineNumber = index + 1
            val isUnterminatedFinalLine = endsWithoutNewline && index == lines.lastIndex
            val entry = if (isUnterminatedFinalLine) {
                // A final line the writer never newline-terminated is a torn
                // write from an interrupted append: recover the committed
                // prefix instead of discarding the whole history.
                decodeOrNull(trimmed) ?: run {
                    trailingCorruption = TornTrailingAuditRecord(
                        lineNumber = lineNumber,
                        recoveredEntryCount = entries.size,
                    )
                    return@forEachIndexed
                }
            } else {
                // Newline-terminated records were fully written; failing to
                // decode one is real corruption and stays a hard failure.
                decode(trimmed, lineNumber = lineNumber)
            }
            if (!seenIds.add(entry.id)) {
                throw AuditStoreException(
                    AuditStoreFailure.DuplicateRecordId(lineNumber = lineNumber),
                )
            }
            entries += entry
        }

        return AuditStoreSnapshot(
            entries = entries,
            entryIds = seenIds,
            trailingCorruption = trailingCorruption,
            endsWithoutNewline = endsWithoutNewline,
        )
    }

    private fun readText(): String {
        val storeSize = try {
            Files.size(storePath)
        } catch (error: IOException) {
            throw AuditStoreException(AuditStoreFailure.Unreadable, error)
        }
        if (storeSize > maxStoreSizeBytes) {
            throw AuditStoreException(AuditStoreFailure.StoreTooLarge)
        }

        return try {
            String(Files.readAllBytes(storePath), StandardCharsets.UTF_8)
        } catch (error: IOException) {
            throw AuditStoreException(AuditStoreFailure.Unreadable, error)
        }
    }

    private fun decode(
        line: String,
        lineNumber: Int,
    ): AuditEntry = try {
        AuditRecordJson.decode(line)
    } catch (error: IllegalArgumentException) {
        throw AuditStoreException(AuditStoreFailure.CorruptRecord(lineNumber), error)
    } catch (error: RuntimeException) {
        throw AuditStoreException(AuditStoreFailure.CorruptRecord(lineNumber), error)
    }

    private fun decodeOrNull(line: String): AuditEntry? = try {
        AuditRecordJson.decode(line)
    } catch (error: RuntimeException) {
        null
    }

    private data class AuditStoreSnapshot(
        val entries: List<AuditEntry>,
        val entryIds: MutableSet<String>,
        val trailingCorruption: TornTrailingAuditRecord? = null,
        val endsWithoutNewline: Boolean = false,
    )

    companion object {
        const val DEFAULT_MAX_STORE_SIZE_BYTES: Long = 10L * 1024 * 1024

        private val pathLocks = ConcurrentHashMap<Path, ReentrantLock>()

        private fun pathLockFor(storePath: Path): ReentrantLock = pathLocks.computeIfAbsent(storePath.toAbsolutePath().normalize()) {
            ReentrantLock()
        }
    }
}
