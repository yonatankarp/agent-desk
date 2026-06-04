package com.yonatankarp.agentdesk.app.runtime

class RuntimeWorkEventImportException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
