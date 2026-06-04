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
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DesktopRuntimeStateProviderTest {
    @Test
    fun `loads stored event state from public-safe config`() {
        val directory = Files.createTempDirectory("agent-desk-desktop-test")
        val eventStore = directory.resolve("events.ndjson")
        val config = directory.resolve("agent-desk.config.properties")
        eventStore.writeText(WorkEventJson.encode(startedEvent()) + "\n")
        config.writeText(
            """
            mode=stored-events
            source=local-event-store
            eventStoreLocation=$eventStore
            """.trimIndent(),
        )

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
}
