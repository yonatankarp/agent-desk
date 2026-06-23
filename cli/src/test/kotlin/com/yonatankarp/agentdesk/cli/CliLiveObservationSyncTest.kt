package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.app.runtime.RuntimeHostProfile
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostReachabilityDiagnostic
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostReachabilityDiagnostics
import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files
import kotlin.io.path.readLines

class CliLiveObservationSyncTest :
    BehaviorSpec({
        given("sync-live-observations") {
            `when`("a reachable read-only host bridge has sanitized observations") {
                then("it imports live observations and reports fresh sync without private details") {
                    val fixture = liveSyncFixture(SANITIZED_OBSERVATION_EXPORT)

                    val result = runLiveSync(fixture)

                    result.exitCode shouldBe 0
                    result.error shouldBe ""
                    assertSoftly(result.output) {
                        shouldContain("Live observation sync: host=host:primary state=synced")
                        shouldContain("freshness=fresh")
                        shouldContain("last-successful-sync=2026-06-06T09:30:00Z")
                        shouldContain("Imported 2 live observation event(s); skipped 0 duplicate event(s).")
                        shouldContain("Diagnostics: imported=2 skipped-duplicate=0")
                        shouldNotContain("agent.desk.host")
                        shouldNotContain(fixture.observationsPath.toString())
                    }
                    result.output.shouldBePublicSafe()
                    fixture.eventStorePath.readLines().map { it.substringAfter("\"type\":\"").substringBefore("\"") } shouldBe
                        listOf("work.started", "work.blocked")
                }
            }

            `when`("the same bridge sync runs twice") {
                then("it skips duplicate observations on the second run") {
                    val fixture = liveSyncFixture(SANITIZED_OBSERVATION_EXPORT)
                    runLiveSync(fixture).exitCode shouldBe 0

                    val result = runLiveSync(fixture)

                    result.exitCode shouldBe 0
                    result.output shouldContain "Imported 0 live observation event(s); skipped 2 duplicate event(s)."
                    result.output shouldContain "Diagnostics: imported=0 skipped-duplicate=2"
                    result.output.shouldBePublicSafe()
                    fixture.eventStorePath.readLines().shouldHaveSize(2)
                }
            }

            `when`("the bridge is reachable but has no current observations") {
                then("it reports stale sync with a safe failure category") {
                    val fixture = liveSyncFixture("""{"schemaVersion":1,"observations":[]}""")

                    val result = runLiveSync(fixture)

                    result.exitCode shouldBe 1
                    result.output shouldContain
                        "Live observation sync: host=host:primary state=stale failure=no-observations"
                    result.output shouldContain "last-successful-sync=unavailable"
                    result.output shouldContain "Diagnostics: imported=0 skipped-duplicate=0"
                    result.output.shouldBePublicSafe()
                    result.error shouldBe ""
                }
            }

            `when`("the configured host is unreachable") {
                then("it refuses sync and reports the reachability category") {
                    val fixture = liveSyncFixture(SANITIZED_OBSERVATION_EXPORT)

                    val result = runLiveSync(fixture) { profile ->
                        RuntimeHostReachabilityDiagnostics.unreachable(profile.alias)
                    }

                    result.exitCode shouldBe 1
                    assertSoftly(result.output) {
                        shouldContain("Host reachability: host=host:primary state=unreachable")
                        shouldContain("failure=network-unavailable")
                        shouldContain("Live observation sync: host=host:primary state=unreachable")
                        shouldNotContain("agent.desk.host")
                    }
                    result.output.shouldBePublicSafe()
                    fixture.eventStorePath.readLines().shouldHaveSize(0)
                }
            }

            `when`("auth is rejected") {
                then("it refuses observation reads before touching the bridge") {
                    val fixture = liveSyncFixture(
                        export = SANITIZED_OBSERVATION_EXPORT,
                        authState = "rejected",
                    )

                    val result = runLiveSync(fixture)

                    result.exitCode shouldBe 1
                    result.output shouldContain "Host access: host=host:primary auth=rejected"
                    result.output shouldContain "failure=authentication-rejected"
                    result.output.shouldBePublicSafe()
                    fixture.eventStorePath.readLines().shouldHaveSize(0)
                }
            }

            `when`("the bridge payload is unsafe") {
                then("it rejects the payload without echoing local path or raw content") {
                    val fixture = liveSyncFixture("""{"rawTranscript":"${privateLinuxPath("private-token.txt")}"}""")

                    val result = runLiveSync(fixture)

                    result.exitCode shouldBe 1
                    result.output shouldContain
                        "Live observation sync: host=host:primary state=unsafe-rejected failure=unsafe-payload-rejected"
                    result.output shouldContain "Runtime observations could not be imported."
                    result.output shouldNotContain (fixture.observationsPath.toString())
                    result.output shouldNotContain (privateLinuxPath("private-token.txt"))
                    result.output.shouldBePublicSafe()
                }
            }
        }
    })

private data class LiveSyncFixture(
    val hostConfigPath: java.nio.file.Path,
    val eventStorePath: java.nio.file.Path,
    val observationsPath: java.nio.file.Path,
)

private fun liveSyncFixture(
    export: String,
    authState: String = "accepted",
    permissionMode: String = "read-only-observation",
): LiveSyncFixture {
    val hostConfig = Files.createTempFile("agent-desk-host-sync", ".properties")
    val observations = Files.createTempFile("agent-desk-host-observations", ".json")
    val eventStore = Files.createTempFile("agent-desk-host-events", ".ndjson")
    Files.writeString(observations, export)
    Files.writeString(
        hostConfig,
        """
        hostAlias=host:primary
        hostEndpoint=https://agent.desk.host:8443/status
        hostAuthState=$authState
        hostPermissionMode=$permissionMode
        hostObservationBridge=$observations
        """.trimIndent(),
    )
    return LiveSyncFixture(hostConfig, eventStore, observations)
}

private fun runLiveSync(
    fixture: LiveSyncFixture,
    check: (RuntimeHostProfile) -> RuntimeHostReachabilityDiagnostic = {
        RuntimeHostReachabilityDiagnostics.reachable(it.alias)
    },
): CliRunResult = runCli(
    "sync-live-observations",
    "--host-config",
    fixture.hostConfigPath.toString(),
    "--event-store",
    fixture.eventStorePath.toString(),
    hostReachabilityCheck = check,
)
