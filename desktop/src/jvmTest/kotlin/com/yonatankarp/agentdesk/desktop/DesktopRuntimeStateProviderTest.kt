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
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DesktopRuntimeStateProviderTest {
    @Test
    fun `loads stored event state from public-safe config`() {
        val directory = Files.createTempDirectory(testRoot(), "agent-desk-desktop-test")
        val eventStore = directory.resolve("events.ndjson")
        val config = directory.resolve("agent-desk.config.properties")
        eventStore.writeText(WorkEventJson.encode(startedEvent()) + "\n")
        writeStoredEventsConfig(config, eventStore)

        val screenState = DesktopRuntimeStateProvider.load(arrayOf("--config", config.toString()))

        val ready = assertIs<DesktopScreenState.Ready>(screenState)
        assertEquals("Loaded state", ready.modeLabel)
        assertContains(DesktopSmokeSnapshotBuilder.from(ready).flattenedText(), "Loaded from sanitized store")
    }

    @Test
    fun `invalid args return public-safe error state`() {
        val screenState = DesktopRuntimeStateProvider.load(arrayOf("--events", "agent-desk-events.ndjson"))

        val error = assertIs<DesktopScreenState.Error>(screenState)
        assertEquals("Usage: agent-desk-desktop [--config <properties-file>]", error.message)
    }

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
}
