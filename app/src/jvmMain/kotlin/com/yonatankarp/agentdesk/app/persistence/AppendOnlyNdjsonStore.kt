package com.yonatankarp.agentdesk.app.persistence

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
 * Shared append-only NDJSON store mechanics (#264 semantics): appends
 * serialize through a process-local path lock and a cooperative file lock,
 * then re-read the store before writing. Record types, codecs, and failure
 * wording stay with each repository via [NdjsonStoreFailures]; the primitive
 * never forms messages and never stringifies record ids.
 */
internal class AppendOnlyNdjsonStore<R : Any, ID : Any>(
    private val storePath: Path,
    private val maxStoreSizeBytes: Long,
    private val encode: (R) -> String,
    private val decode: (String) -> R,
    private val idOf: (R) -> ID,
    private val failures: NdjsonStoreFailures<ID>,
) {
    fun append(record: R) {
        try {
            storePath.parent?.let(Files::createDirectories)
        } catch (error: IOException) {
            throw failures.appendFailed(error)
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
                        snapshot.tornTrailingLineNumber?.let { lineNumber ->
                            throw failures.appendBlockedByTornRecord(
                                lineNumber = lineNumber,
                                recoveredRecordCount = snapshot.records.size,
                            )
                        }
                        val recordId = idOf(record)
                        if (recordId in snapshot.recordIds) {
                            throw failures.duplicateId(id = recordId, lineNumber = null)
                        }

                        channel.position(channel.size())
                        val isolatingNewline = if (snapshot.endsWithoutNewline) "\n" else ""
                        val bytes = ByteBuffer.wrap(
                            (isolatingNewline + encode(record) + "\n").toByteArray(StandardCharsets.UTF_8),
                        )
                        while (bytes.hasRemaining()) {
                            channel.write(bytes)
                        }
                        channel.force(false)
                    }
                }
            } catch (error: IOException) {
                throw failures.appendFailed(error)
            } catch (error: OverlappingFileLockException) {
                throw failures.appendFailed(error)
            }
        }
    }

    fun readSnapshot(): NdjsonStoreSnapshot<R, ID> {
        if (!Files.exists(storePath)) {
            return NdjsonStoreSnapshot(records = emptyList(), recordIds = emptySet())
        }

        val text = readText()
        val endsWithoutNewline = text.isNotEmpty() && !text.endsWith("\n")
        val lines = text.split("\n")

        val seenIds = mutableSetOf<ID>()
        val records = mutableListOf<R>()
        var tornTrailingLineNumber: Int? = null
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                return@forEachIndexed
            }

            val lineNumber = index + 1
            val isUnterminatedFinalLine = endsWithoutNewline && index == lines.lastIndex
            val record = if (isUnterminatedFinalLine) {
                // A final line the writer never newline-terminated is a torn
                // write from an interrupted append: recover the committed
                // prefix instead of discarding the whole history.
                decodeOrNull(trimmed) ?: run {
                    tornTrailingLineNumber = lineNumber
                    return@forEachIndexed
                }
            } else {
                // Newline-terminated records were fully written; failing to
                // decode one is real corruption and stays a hard failure.
                decodeCommitted(trimmed, lineNumber = lineNumber)
            }
            if (!seenIds.add(idOf(record))) {
                throw failures.duplicateId(id = idOf(record), lineNumber = lineNumber)
            }
            records += record
        }

        return NdjsonStoreSnapshot(
            records = records,
            recordIds = seenIds,
            tornTrailingLineNumber = tornTrailingLineNumber,
            endsWithoutNewline = endsWithoutNewline,
        )
    }

    private fun readText(): String {
        val storeSize = try {
            Files.size(storePath)
        } catch (error: IOException) {
            throw failures.unreadable(error)
        }
        if (storeSize > maxStoreSizeBytes) {
            throw failures.storeTooLarge()
        }

        return try {
            String(Files.readAllBytes(storePath), StandardCharsets.UTF_8)
        } catch (error: IOException) {
            throw failures.unreadable(error)
        }
    }

    private fun decodeCommitted(
        line: String,
        lineNumber: Int,
    ): R = try {
        decode(line)
    } catch (error: IllegalArgumentException) {
        throw failures.corruptRecord(lineNumber, error)
    } catch (error: RuntimeException) {
        throw failures.corruptRecord(lineNumber, error)
    }

    private fun decodeOrNull(line: String): R? = try {
        decode(line)
    } catch (error: RuntimeException) {
        null
    }

    companion object {
        const val DEFAULT_MAX_STORE_SIZE_BYTES: Long = 10L * 1024 * 1024

        /**
         * One lock per normalized store path, shared across all record types
         * in-process; entries stay resident for the process lifetime.
         */
        private val pathLocks = ConcurrentHashMap<Path, ReentrantLock>()

        private fun pathLockFor(storePath: Path): ReentrantLock = pathLocks.computeIfAbsent(storePath.toAbsolutePath().normalize()) {
            ReentrantLock()
        }
    }
}
