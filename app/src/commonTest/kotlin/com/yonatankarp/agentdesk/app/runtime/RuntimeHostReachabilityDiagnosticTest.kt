package com.yonatankarp.agentdesk.app.runtime

import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class RuntimeHostReachabilityDiagnosticTest :
    BehaviorSpec({
        given("runtime host reachability diagnostics") {
            `when`("every public state is rendered") {
                then("operators can distinguish the failure mode without endpoint details") {
                    val alias = RuntimeHostAlias.parse("office-lab-host")
                    val diagnostics = listOf(
                        RuntimeHostReachabilityDiagnostics.notConfigured(),
                        RuntimeHostReachabilityDiagnostics.reachable(alias),
                        RuntimeHostReachabilityDiagnostics.unreachable(alias),
                        RuntimeHostReachabilityDiagnostics.timedOut(alias),
                        RuntimeHostReachabilityDiagnostics.rejected(alias),
                        RuntimeHostReachabilityDiagnostics.unsupportedHostMode(alias),
                        RuntimeHostReachabilityDiagnostics.unsafePrivateDetailRedacted(alias),
                    )

                    diagnostics.map { it.state }.shouldContainExactly(
                        RuntimeHostReachabilityState.NotConfigured,
                        RuntimeHostReachabilityState.Reachable,
                        RuntimeHostReachabilityState.Unreachable,
                        RuntimeHostReachabilityState.TimedOut,
                        RuntimeHostReachabilityState.Rejected,
                        RuntimeHostReachabilityState.UnsupportedHostMode,
                        RuntimeHostReachabilityState.UnsafePrivateDetailRedacted,
                    )
                    diagnostics.map { it.publicMessage() }.forEach { message ->
                        message.shouldBePublicSafe()
                    }
                    diagnostics.map { it.publicMessage() }.shouldContainExactly(
                        "Host reachability: host=not-configured state=not-configured failure=missing-configuration.",
                        "Host reachability: host=office-lab-host state=reachable.",
                        "Host reachability: host=office-lab-host state=unreachable failure=network-unavailable.",
                        "Host reachability: host=office-lab-host state=timed-out failure=timeout.",
                        "Host reachability: host=office-lab-host state=rejected failure=authentication-rejected.",
                        "Host reachability: host=office-lab-host state=unsupported-host-mode failure=unsupported-host-mode.",
                        "Host reachability: host=office-lab-host state=unsafe-private-detail-redacted " +
                            "failure=unsafe-private-detail-redacted private-detail=redacted.",
                    )
                }
            }

            `when`("raw endpoint details are offered as aliases") {
                then("validation rejects them without echoing the private value") {
                    unsafeAliases().forEach { unsafe ->
                        val error = shouldThrow<RuntimeHostReachabilityException> {
                            RuntimeHostAlias.parse(unsafe)
                        }

                        assertSoftly(error.message.orEmpty()) {
                            shouldContain("hostAlias")
                            shouldNotContain(unsafe)
                            shouldNotContain(rawIdentifier())
                        }
                    }
                }
            }

            `when`("a diagnostic includes unsafe source details") {
                then("the public contract represents only the redacted detail state") {
                    val alias = RuntimeHostAlias.parse("operator-lab")
                    val diagnostic = RuntimeHostReachabilityDiagnostics.unsafePrivateDetailRedacted(alias)

                    diagnostic.privateDetailRedacted shouldBe true
                    diagnostic.publicMessage().shouldBePublicSafe()
                    assertSoftly(diagnostic.publicMessage()) {
                        shouldContain("operator-lab")
                        shouldContain("private-detail=redacted")
                        shouldNotContain("/home/")
                        shouldNotContain("192.168.")
                        shouldNotContain("private-token")
                    }
                }
            }

            `when`("state and failure categories are combined inconsistently") {
                then("contract validation fails before a diagnostic can be published") {
                    shouldThrow<RuntimeHostReachabilityException> {
                        RuntimeHostReachabilityDiagnostic(
                            state = RuntimeHostReachabilityState.Reachable,
                            alias = RuntimeHostAlias.parse("operator-lab"),
                            failure = RuntimeHostReachabilityFailure.NetworkUnavailable,
                        )
                    }.message shouldBe "reachable diagnostics must not include a failure"

                    shouldThrow<RuntimeHostReachabilityException> {
                        RuntimeHostReachabilityDiagnostic(
                            state = RuntimeHostReachabilityState.Unreachable,
                            alias = RuntimeHostAlias.parse("operator-lab"),
                        )
                    }.message shouldBe "failed host diagnostics require a failure category"

                    shouldThrow<RuntimeHostReachabilityException> {
                        RuntimeHostReachabilityDiagnostic(
                            state = RuntimeHostReachabilityState.TimedOut,
                            alias = RuntimeHostAlias.parse("operator-lab"),
                            failure = RuntimeHostReachabilityFailure.NetworkUnavailable,
                        )
                    }.message shouldBe "host reachability state and failure category must match"

                    shouldThrow<RuntimeHostReachabilityException> {
                        RuntimeHostReachabilityDiagnostic(
                            state = RuntimeHostReachabilityState.Rejected,
                            alias = RuntimeHostAlias.parse("operator-lab"),
                            failure = RuntimeHostReachabilityFailure.Timeout,
                        )
                    }.message shouldBe "host reachability state and failure category must match"

                    shouldThrow<RuntimeHostReachabilityException> {
                        RuntimeHostReachabilityDiagnostic(
                            state = RuntimeHostReachabilityState.UnsafePrivateDetailRedacted,
                            alias = RuntimeHostAlias.parse("operator-lab"),
                            failure = RuntimeHostReachabilityFailure.UnsafePrivateDetailRedacted,
                        )
                    }.message shouldBe "unsafe-private-detail-redacted diagnostics require redaction"

                    shouldThrow<RuntimeHostReachabilityException> {
                        RuntimeHostReachabilityDiagnostic(
                            state = RuntimeHostReachabilityState.Unreachable,
                            alias = RuntimeHostAlias.parse("operator-lab"),
                            failure = RuntimeHostReachabilityFailure.NetworkUnavailable,
                            privateDetailRedacted = true,
                        )
                    }.message shouldBe "only unsafe-private-detail-redacted diagnostics may mark private detail redacted"
                }
            }
        }
    })

private fun unsafeAliases(): List<String> = listOf(
    privateLinuxPath("host"),
    privateMacPath("host"),
    windowsUserPath("host"),
    "https://localhost:8443",
    "http://192.168.1.20:8443",
    "office-lab:8443",
    "auth_token=value",
    "private-token",
    "channel:${rawIdentifier()}",
)

private fun privateLinuxPath(fileName: String): String = "/home/" + "operator/$fileName"

private fun privateMacPath(fileName: String): String = "/Users/" + "operator/$fileName"

private fun windowsUserPath(fileName: String): String = listOf("C:", "Users", "operator", "AgentDesk", fileName).joinToString("\\")

private fun rawIdentifier(): String = "123456789" + "012345678"
