package com.yonatankarp.agentdesk.app.runtime

class RuntimeWorkEventImportException(
    message: String,
    val diagnostics: List<RuntimeWorkEventImportDiagnostic> = emptyList(),
    cause: Throwable? = null,
) : RuntimeException(message, cause)
