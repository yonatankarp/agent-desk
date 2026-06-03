package com.yonatankarp.agentdesk.app.operator

class OperatorStateLoadException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
