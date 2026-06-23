package com.yonatankarp.agentdesk.app.runtime

enum class RuntimeHostAuthState(val wireName: String) {
    NotConfigured("not-configured"),
    Pending("pending"),
    Accepted("accepted"),
    Rejected("rejected"),
    Expired("expired"),
    Unsupported("unsupported"),
    ;

    companion object {
        fun parse(raw: String): RuntimeHostAuthState = entries.firstOrNull { it.wireName == raw.trim() }
            ?: throw RuntimeHostReachabilityException("hostAuthState is not supported")
    }
}
