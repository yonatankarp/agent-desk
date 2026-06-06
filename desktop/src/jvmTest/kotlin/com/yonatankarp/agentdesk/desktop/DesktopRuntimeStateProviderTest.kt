package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.app.serialization.WorkEventJson
import com.yonatankarp.agentdesk.core.domain.events.EventSource
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary
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

            `when`("loading with invalid args") {
                then("it returns a public-safe error state") {
                    val screenState =
                        DesktopRuntimeStateProvider.load(arrayOf("--events", "agent-desk-events.ndjson"))

                    val error = screenState.shouldBeInstanceOf<DesktopScreenState.Error>()
                    error.message shouldBe "Usage: agent-desk-desktop [--config <properties-file>]"
                }
            }
        }
    })

private fun startedEvent(): WorkEvent = WorkEvent(
    id = WorkEventId.parse("event:agent-task:77:started"),
    occurredAt = EventTimestamp.parse("2026-06-02T21:00:00Z"),
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
