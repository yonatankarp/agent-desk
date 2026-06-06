package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.app.config.AgentDeskMode
import com.yonatankarp.agentdesk.app.config.AgentDeskRuntimeConfig
import com.yonatankarp.agentdesk.app.config.EventStoreLocation
import com.yonatankarp.agentdesk.app.config.RuntimeEventSourceKind
import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.SampleOperatorState
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DesktopStateResolverTest {
    @Test
    fun `sample config resolves sample screen state`() {
        val screenState = DesktopStateResolver { SampleOperatorState.current() }.resolve(AgentDeskRuntimeConfig.defaults())

        val ready = assertIs<DesktopScreenState.Ready>(screenState)
        assertEquals("Sample state", ready.modeLabel)
        assertContains(DesktopSmokeSnapshotBuilder.from(ready).flattenedText(), "Work state")
    }

    @Test
    fun `stored-event config resolves loaded screen state`() {
        val screenState =
            DesktopStateResolver { OperatorState(workItems = emptyList(), events = emptyList()) }
                .resolve(storedEventConfig())

        val ready = assertIs<DesktopScreenState.Ready>(screenState)
        assertEquals("Loaded state", ready.modeLabel)
        assertEquals("0 active / 0 attention", DesktopSmokeSnapshotBuilder.from(ready).summary)
    }

    @Test
    fun `loading and error states have public-safe snapshot rows`() {
        val loading = DesktopSmokeSnapshotBuilder.from(DesktopScreenState.Loading)
        val error = DesktopSmokeSnapshotBuilder.from(DesktopScreenState.Error("Configured event store could not be read."))

        assertEquals(listOf("Loading operator state"), loading.sectionRows("Decision queue"))
        assertEquals(listOf("Configured event store could not be read."), error.sectionRows("Decision queue"))
        assertEquals(listOf("Replay status: loading operator state."), loading.sectionRows("Replay status"))
        assertEquals(listOf("Blocked/error: Configured event store could not be read."), error.sectionRows("Replay status"))
    }

    @Test
    fun `loader failures resolve sanitized error state`() {
        val screenState =
            DesktopStateResolver { error("boom with private details") }
                .resolve(storedEventConfig())

        val error = assertIs<DesktopScreenState.Error>(screenState)
        assertEquals("Configured operator state could not be loaded.", error.message)
    }

    private fun storedEventConfig(): AgentDeskRuntimeConfig = AgentDeskRuntimeConfig(
        mode = AgentDeskMode.StoredEvents,
        source = RuntimeEventSourceKind.LocalEventStore,
        eventStoreLocation = EventStoreLocation.parse("agent-desk-events.ndjson"),
    )

    private fun DesktopSmokeSnapshot.sectionRows(title: String): List<String> = sections.single { it.title == title }.rows
}
