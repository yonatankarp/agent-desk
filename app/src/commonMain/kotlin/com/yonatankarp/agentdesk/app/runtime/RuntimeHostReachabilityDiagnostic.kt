package com.yonatankarp.agentdesk.app.runtime

data class RuntimeHostReachabilityDiagnostic(
    val state: RuntimeHostReachabilityState,
    val alias: RuntimeHostAlias? = null,
    val failure: RuntimeHostReachabilityFailure? = null,
    val privateDetailRedacted: Boolean = false,
) {
    init {
        if (state == RuntimeHostReachabilityState.NotConfigured && alias != null) {
            throw RuntimeHostReachabilityException("not-configured diagnostics must not include a host alias")
        }
        if (state == RuntimeHostReachabilityState.NotConfigured && failure != RuntimeHostReachabilityFailure.MissingConfiguration) {
            throw RuntimeHostReachabilityException("not-configured diagnostics require a missing-configuration failure")
        }
        if (state != RuntimeHostReachabilityState.NotConfigured && alias == null) {
            throw RuntimeHostReachabilityException("configured host diagnostics require a host alias")
        }
        if (state == RuntimeHostReachabilityState.Reachable && failure != null) {
            throw RuntimeHostReachabilityException("reachable diagnostics must not include a failure")
        }
        if (state != RuntimeHostReachabilityState.Reachable && state != RuntimeHostReachabilityState.NotConfigured && failure == null) {
            throw RuntimeHostReachabilityException("failed host diagnostics require a failure category")
        }
        if (state == RuntimeHostReachabilityState.UnsafePrivateDetailRedacted && !privateDetailRedacted) {
            throw RuntimeHostReachabilityException("unsafe-private-detail-redacted diagnostics require redaction")
        }
    }

    fun publicMessage(): String {
        val target = alias?.let { "host=${it.value}" } ?: "host=not-configured"
        val failureText = failure?.let { " failure=${it.wireName}" }.orEmpty()
        val redactionText = if (privateDetailRedacted) " private-detail=redacted" else ""
        return "Host reachability: $target state=${state.wireName}$failureText$redactionText."
    }
}

enum class RuntimeHostReachabilityState(val wireName: String) {
    NotConfigured("not-configured"),
    Reachable("reachable"),
    Unreachable("unreachable"),
    TimedOut("timed-out"),
    Rejected("rejected"),
    UnsupportedHostMode("unsupported-host-mode"),
    UnsafePrivateDetailRedacted("unsafe-private-detail-redacted"),
}

enum class RuntimeHostReachabilityFailure(val wireName: String) {
    MissingConfiguration("missing-configuration"),
    NetworkUnavailable("network-unavailable"),
    AuthenticationRejected("authentication-rejected"),
    UnsupportedHostMode("unsupported-host-mode"),
    Timeout("timeout"),
    UnsafePrivateDetailRedacted("unsafe-private-detail-redacted"),
}

class RuntimeHostReachabilityException(
    message: String,
) : RuntimeException(message)
