package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.app.runtime.RuntimeHostProfile
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostReachabilityDiagnostic
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostReachabilityDiagnostics
import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files

class CliHostSmokeTest :
    BehaviorSpec({
        given("host-smoke") {
            `when`("the configured host is reachable") {
                then("it prints a public-safe diagnostic and exits zero") {
                    val result = runHostSmoke { profile ->
                        RuntimeHostReachabilityDiagnostics.reachable(profile.alias)
                    }

                    result.exitCode shouldBe 0
                    result.output shouldBe "Host reachability: host=host:primary state=reachable."
                    result.output.shouldBePublicSafe()
                    result.error shouldBe ""
                }
            }

            `when`("the configured host is unreachable") {
                then("it exits non-zero with a public-safe failure category") {
                    val result = runHostSmoke { profile ->
                        RuntimeHostReachabilityDiagnostics.unreachable(profile.alias)
                    }

                    result.exitCode shouldBe 1
                    assertSoftly(result.output) {
                        shouldContain("host=host:primary")
                        shouldContain("state=unreachable")
                        shouldContain("failure=network-unavailable")
                        shouldNotContain("agent.desk.host")
                        shouldNotContain("8443")
                    }
                    result.output.shouldBePublicSafe()
                    result.error shouldBe ""
                }
            }

            `when`("the configured host times out") {
                then("it exits non-zero with the timeout category") {
                    val result = runHostSmoke { profile ->
                        RuntimeHostReachabilityDiagnostics.timedOut(profile.alias)
                    }

                    result.exitCode shouldBe 1
                    result.output shouldContain "state=timed-out"
                    result.output shouldContain "failure=timeout"
                    result.output.shouldBePublicSafe()
                    result.error shouldBe ""
                }
            }

            `when`("host config is missing required fields") {
                then("it renders not-configured without leaking local config details") {
                    val path = Files.createTempFile("agent-desk-host-smoke", ".properties")
                    Files.writeString(path, "hostAlias=host:primary\n")

                    val result = runCli("host-smoke", "--host-config", path.toString())

                    result.exitCode shouldBe 1
                    result.output shouldBe
                        "Host reachability: host=not-configured state=not-configured failure=missing-configuration."
                    result.output.shouldBePublicSafe()
                    result.error shouldBe ""
                }
            }
        }
    })

private fun runHostSmoke(
    check: (RuntimeHostProfile) -> RuntimeHostReachabilityDiagnostic,
): CliRunResult {
    val path = Files.createTempFile("agent-desk-host-smoke", ".properties")
    Files.writeString(
        path,
        """
        hostAlias=host:primary
        hostEndpoint=https://agent.desk.host:8443/status
        """.trimIndent(),
    )
    return runCli("host-smoke", "--host-config", path.toString(), hostReachabilityCheck = check)
}
