package com.yonatankarp.agentdesk.app.persistence

import com.yonatankarp.agentdesk.app.operator.audit.AuditEntry

/**
 * Result of reading the audit store: the committed entries plus an optional
 * trailing-corruption warning when a torn final record was tolerated.
 */
data class AuditRecordReadResult(
    val entries: List<AuditEntry>,
    val trailingCorruption: TornTrailingAuditRecord? = null,
)

/**
 * Public-safe signal that the final audit record was torn by an interrupted
 * write. Carries only the literal line number and the recovered entry count;
 * never the store path, file size, or record content.
 */
data class TornTrailingAuditRecord(
    val lineNumber: Int,
    val recoveredEntryCount: Int,
) {
    fun publicSafeMessage(): String = "Torn trailing record at line $lineNumber in configured audit store; " +
        "recovered $recoveredEntryCount committed audit record(s). " +
        "Appending is blocked until the store is repaired."
}
