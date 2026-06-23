package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostAlias
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostReachabilityDiagnostics
import com.yonatankarp.agentdesk.app.serialization.WorkEventJson
import com.yonatankarp.agentdesk.core.domain.events.EventSource
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary
import com.yonatankarp.agentdesk.testfixtures.WorkEventFixtures
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.writeText

class DesktopRuntimeStateProviderTest :
    BehaviorSpec({
        given("the desktop runtime state provider") {
            `when`("loading from a public-safe stored-event config") {
                then("it loads the stored event state") {
                    val directory = Files.createTempDirectory(testRoot(), "agent-desk-desktop-test")
                    val eventStore = directory.resolve("events.ndjson")
                    val config = directory.resolve("agent-desk.config.properties")
                    eventStore.writeText(WorkEventJson.encode(startedEvent()) + "\n")
                    writeStoredEventsConfig(config, eventStore)

                    val screenState =
                        DesktopRuntimeStateProvider.load(arrayOf("--config", config.toString()))

                    val ready = screenState.shouldBeInstanceOf<DesktopScreenState.Ready>()
                    ready.modeLabel shouldBe "Loaded state"
                    DesktopSmokeSnapshotBuilder.from(ready).flattenedText() shouldContain
                        "Loaded from sanitized store"
                }
            }

            `when`("loading with a host config and default state") {
                then("it includes a public not-configured host diagnostic") {
                    val directory = Files.createTempDirectory(testRoot(), "agent-desk-desktop-host-test")
                    val hostConfig = directory.resolve("agent-desk.host.properties")
                    hostConfig.writeText("")

                    val screenState =
                        DesktopRuntimeStateProvider.load(arrayOf("--host-config", hostConfig.toString()))

                    val ready = screenState.shouldBeInstanceOf<DesktopScreenState.Ready>()
                    val rows = DesktopReplayStatus.rows(ready).joinToString("\n")
                    rows shouldContain "Health: Source disconnected."
                    rows shouldContain
                        "Diagnostic: Host reachability: host=not-configured state=not-configured failure=missing-configuration."
                }
            }

            `when`("desktop replay rows receive an unreachable host diagnostic") {
                then("they show the host alias without endpoint details") {
                    val rows = DesktopReplayStatus.rows(
                        DesktopScreenState.Ready(
                            state = OperatorState(
                                workItems = emptyList(),
                                events = emptyList(),
                                hostConnectivity = RuntimeHostReachabilityDiagnostics.unreachable(
                                    RuntimeHostAlias.parse("host:primary"),
                                ),
                            ),
                            modeLabel = "Sample state",
                        ),
                    ).joinToString("\n")

                    rows shouldContain "Health: Source disconnected."
                    rows shouldContain
                        "Diagnostic: Host reachability: host=host:primary state=unreachable failure=network-unavailable."
                }
            }

            `when`("desktop replay rows receive a reachable host diagnostic") {
                then("they show reachable host status") {
                    val rows = DesktopReplayStatus.rows(
                        DesktopScreenState.Ready(
                            state = OperatorState(
                                workItems = emptyList(),
                                events = emptyList(),
                                hostConnectivity = RuntimeHostReachabilityDiagnostics.reachable(
                                    RuntimeHostAlias.parse("host:primary"),
                                ),
                            ),
                            modeLabel = "Sample state",
                        ),
                    ).joinToString("\n")

                    rows shouldContain "Health: Empty."
                    rows shouldContain "Diagnostic: Host reachability: host=host:primary state=reachable."
                }
            }

            `when`("loading with invalid args") {
                then("it returns a public-safe error state") {
                    val screenState =
                        DesktopRuntimeStateProvider.load(arrayOf("--events", "agent-desk-events.ndjson"))

                    val error = screenState.shouldBeInstanceOf<DesktopScreenState.Error>()
                    error.message shouldBe
                        "Usage: agent-desk-desktop [--config <properties-file>] [--host-config <properties-file>]"
                }
            }
        }
    })

private fun startedEvent() = WorkEventFixtures.workStartedEvent(
    id = WorkEventId.parse("event:agent-task:77:started"),
    source = EventSource.parse("test-store"),
    workItemId = WorkItemId.parse("agent-task:77"),
    payload = WorkStartedPayload(
        title = WorkItemTitle.parse("Loaded from sanitized store"),
        summary = WorkSummary.parse("Desktop loaded projected operator state."),
    ),
)

private fun writeStoredEventsConfig(
    config: Path,
    eventStore: Path,
) {
    val properties = Properties().apply {
        setProperty("mode", "stored-events")
        setProperty("source", "local-event-store")
        setProperty("eventStoreLocation", eventStore.toString())
    }
    Files.newBufferedWriter(config).use { writer ->
        properties.store(writer, null)
    }
}

private fun testRoot(): Path {
    val root = Path.of("build", "tmp", "desktop-runtime-state-provider-test")
    Files.createDirectories(root)
    return root
}
