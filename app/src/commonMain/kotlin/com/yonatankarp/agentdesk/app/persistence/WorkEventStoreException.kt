package com.yonatankarp.agentdesk.app.persistence

class WorkEventStoreException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
