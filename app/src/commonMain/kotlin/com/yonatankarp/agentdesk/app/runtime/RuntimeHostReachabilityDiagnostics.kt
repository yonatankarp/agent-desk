package com.yonatankarp.agentdesk.app.runtime

object RuntimeHostReachabilityDiagnostics {
    fun notConfigured(): RuntimeHostReachabilityDiagnostic = RuntimeHostReachabilityDiagnostic(
        state = RuntimeHostReachabilityState.NotConfigured,
        failure = RuntimeHostReachabilityFailure.MissingConfiguration,
    )

    fun reachable(alias: RuntimeHostAlias): RuntimeHostReachabilityDiagnostic = RuntimeHostReachabilityDiagnostic(
        state = RuntimeHostReachabilityState.Reachable,
        alias = alias,
    )

    fun unreachable(alias: RuntimeHostAlias): RuntimeHostReachabilityDiagnostic = RuntimeHostReachabilityDiagnostic(
        state = RuntimeHostReachabilityState.Unreachable,
        alias = alias,
        failure = RuntimeHostReachabilityFailure.NetworkUnavailable,
    )

    fun timedOut(alias: RuntimeHostAlias): RuntimeHostReachabilityDiagnostic = RuntimeHostReachabilityDiagnostic(
        state = RuntimeHostReachabilityState.TimedOut,
        alias = alias,
        failure = RuntimeHostReachabilityFailure.Timeout,
    )

    fun rejected(alias: RuntimeHostAlias): RuntimeHostReachabilityDiagnostic = RuntimeHostReachabilityDiagnostic(
        state = RuntimeHostReachabilityState.Rejected,
        alias = alias,
        failure = RuntimeHostReachabilityFailure.AuthenticationRejected,
    )

    fun unsupportedHostMode(alias: RuntimeHostAlias): RuntimeHostReachabilityDiagnostic = RuntimeHostReachabilityDiagnostic(
        state = RuntimeHostReachabilityState.UnsupportedHostMode,
        alias = alias,
        failure = RuntimeHostReachabilityFailure.UnsupportedHostMode,
    )

    fun unsafePrivateDetailRedacted(alias: RuntimeHostAlias): RuntimeHostReachabilityDiagnostic = RuntimeHostReachabilityDiagnostic(
        state = RuntimeHostReachabilityState.UnsafePrivateDetailRedacted,
        alias = alias,
        failure = RuntimeHostReachabilityFailure.UnsafePrivateDetailRedacted,
        privateDetailRedacted = true,
    )
}
