package com.yonatankarp.agentdesk.app.runtime

data class RuntimeWorkEventImportResult(
    val importedCount: Int,
    val skippedDuplicateCount: Int,
)
