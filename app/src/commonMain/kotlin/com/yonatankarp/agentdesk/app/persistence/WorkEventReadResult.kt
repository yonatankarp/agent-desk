package com.yonatankarp.agentdesk.app.persistence

import com.yonatankarp.agentdesk.core.domain.events.WorkEvent

/**
 * Result of reading the event store: the committed events plus an optional
 * trailing-corruption warning when a torn final record was tolerated.
 */
data class WorkEventReadResult(
    val events: List<WorkEvent>,
    val trailingCorruption: TornTrailingRecord? = null,
)

/**
 * Public-safe signal that the final store record was torn by an interrupted
 * write. Carries only the literal line number and the recovered event count;
 * never the store path, file size, or record content.
 */
data class TornTrailingRecord(
    val lineNumber: Int,
    val recoveredEventCount: Int,
) {
    fun publicSafeMessage(): String = "Torn trailing record at line $lineNumber in configured event store; " +
        "recovered $recoveredEventCount committed event(s). " +
        "Appending is blocked until the store is repaired."
}
