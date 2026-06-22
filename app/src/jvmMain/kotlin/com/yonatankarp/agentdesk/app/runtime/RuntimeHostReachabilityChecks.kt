package com.yonatankarp.agentdesk.app.runtime

import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException

object RuntimeHostReachabilityChecks {
    fun check(
        profile: RuntimeHostProfile,
        timeoutMillis: Int = 2_000,
    ): RuntimeHostReachabilityDiagnostic = try {
        val endpoint = URI(profile.endpoint.value)
        val port = endpoint.port.takeIf { it > 0 } ?: endpoint.scheme.defaultPort()
        val host = endpoint.host ?: return RuntimeHostReachabilityDiagnostics.unsafePrivateDetailRedacted(profile.alias)

        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), timeoutMillis)
        }
        RuntimeHostReachabilityDiagnostics.reachable(profile.alias)
    } catch (exception: SocketTimeoutException) {
        RuntimeHostReachabilityDiagnostics.timedOut(profile.alias)
    } catch (exception: UnknownHostException) {
        RuntimeHostReachabilityDiagnostics.unreachable(profile.alias)
    } catch (exception: SocketException) {
        RuntimeHostReachabilityDiagnostics.unreachable(profile.alias)
    } catch (exception: SecurityException) {
        RuntimeHostReachabilityDiagnostics.unsafePrivateDetailRedacted(profile.alias)
    } catch (exception: RuntimeHostReachabilityException) {
        RuntimeHostReachabilityDiagnostics.unsafePrivateDetailRedacted(profile.alias)
    } catch (exception: RuntimeException) {
        RuntimeHostReachabilityDiagnostics.unreachable(profile.alias)
    }

    private fun String.defaultPort(): Int = when (lowercase()) {
        "http" -> 80
        "https" -> 443
        else -> throw RuntimeHostReachabilityException("hostEndpoint must use http or https")
    }
}
