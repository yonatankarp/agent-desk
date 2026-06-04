package com.yonatankarp.agentdesk.app.operator

enum class OperatorActionIntent(val wireName: String) {
    Resume("resume"),
    Stop("stop"),
    Inspect("inspect"),
    ;

    companion object {
        fun fromWireName(raw: String): OperatorActionIntent = entries.firstOrNull { it.wireName == raw.trim().lowercase() }
            ?: throw OperatorActionException("Unsupported operator action.")
    }
}

class OperatorActionException(
    message: String,
) : RuntimeException(message)
