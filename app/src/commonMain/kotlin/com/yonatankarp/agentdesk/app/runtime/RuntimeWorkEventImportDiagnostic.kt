package com.yonatankarp.agentdesk.app.runtime

data class RuntimeWorkEventImportDiagnostic(
    val kind: RuntimeWorkEventImportDiagnosticKind,
    val message: String,
    val eventId: String? = null,
)

enum class RuntimeWorkEventImportDiagnosticKind {
    Imported,
    SkippedDuplicate,
    InvalidSource,
    UnsafeRejected,
    StoreRejected,
    RedactedOrDropped,
}

fun List<RuntimeWorkEventImportDiagnostic>.summary(): RuntimeWorkEventImportDiagnosticSummary {
    fun count(kind: RuntimeWorkEventImportDiagnosticKind): Int = count { it.kind == kind }

    return RuntimeWorkEventImportDiagnosticSummary(
        imported = count(RuntimeWorkEventImportDiagnosticKind.Imported),
        skippedDuplicate = count(RuntimeWorkEventImportDiagnosticKind.SkippedDuplicate),
        invalid = count(RuntimeWorkEventImportDiagnosticKind.InvalidSource),
        unsafeRejected = count(RuntimeWorkEventImportDiagnosticKind.UnsafeRejected),
        storeRejected = count(RuntimeWorkEventImportDiagnosticKind.StoreRejected),
        redactedOrDropped = count(RuntimeWorkEventImportDiagnosticKind.RedactedOrDropped),
    )
}

data class RuntimeWorkEventImportDiagnosticSummary(
    val imported: Int,
    val skippedDuplicate: Int,
    val invalid: Int,
    val unsafeRejected: Int,
    val storeRejected: Int,
    val redactedOrDropped: Int,
) {
    fun publicMessage(): String = "Diagnostics: imported=$imported skipped-duplicate=$skippedDuplicate " +
        "invalid=$invalid unsafe-rejected=$unsafeRejected " +
        "store-rejected=$storeRejected redacted-or-dropped=$redactedOrDropped."
}
