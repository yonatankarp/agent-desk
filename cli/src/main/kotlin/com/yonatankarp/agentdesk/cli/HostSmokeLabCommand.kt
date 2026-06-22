package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.app.runtime.RuntimeHostAlias
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostEndpoint
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostProfile
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostReachabilityDiagnostics

object HostSmokeLabCommand {
    fun execute(): String {
        val profile = RuntimeHostProfile(
            alias = RuntimeHostAlias.parse("host:lab"),
            endpoint = RuntimeHostEndpoint.parse("https://lab.fixture.host/status"),
        )
        val diagnostics = listOf(
            RuntimeHostReachabilityDiagnostics.reachable(profile.alias),
            RuntimeHostReachabilityDiagnostics.unreachable(profile.alias),
            RuntimeHostReachabilityDiagnostics.timedOut(profile.alias),
            RuntimeHostReachabilityDiagnostics.rejected(profile.alias),
            RuntimeHostReachabilityDiagnostics.unsafePrivateDetailRedacted(profile.alias),
        )
        return buildString {
            appendLine("Host connectivity lab: public-safe simulated diagnostics.")
            diagnostics.forEach { diagnostic ->
                appendLine(diagnostic.publicMessage())
            }
            append("Host connectivity lab passed.")
        }
    }
}
