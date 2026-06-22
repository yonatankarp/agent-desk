package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class CliHostSmokeLabTest :
    BehaviorSpec({
        given("host-smoke-lab") {
            `when`("the CLI runs the simulated lab") {
                then("it renders every required public-safe diagnostic state") {
                    val result = runCli("host-smoke-lab")

                    result.exitCode shouldBe 0
                    result.error shouldBe ""
                    result.output.shouldBePublicSafe()
                    assertSoftly(result.output) {
                        shouldContain("Host connectivity lab: public-safe simulated diagnostics.")
                        shouldContain("Host reachability: host=host:lab state=reachable.")
                        shouldContain("state=unreachable failure=network-unavailable")
                        shouldContain("state=timed-out failure=timeout")
                        shouldContain("state=rejected failure=authentication-rejected")
                        shouldContain("state=unsafe-private-detail-redacted")
                        shouldContain("private-detail=redacted")
                        shouldContain("Host connectivity lab passed.")
                        shouldNotContain("lab.fixture.host")
                    }
                }
            }
        }
    })
