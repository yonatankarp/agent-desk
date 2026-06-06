package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.app.config.AgentDeskMode
import com.yonatankarp.agentdesk.app.config.AgentDeskRuntimeConfig
import com.yonatankarp.agentdesk.app.config.EventStoreLocation
import com.yonatankarp.agentdesk.app.config.RuntimeEventSourceKind
import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.SampleOperatorState
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

class DesktopStateResolverTest :
    BehaviorSpec({
        given("a desktop state resolver") {
            `when`("the sample config is resolved") {
                then("it resolves a sample screen state") {
                    val screenState =
                        DesktopStateResolver { SampleOperatorState.current() }
                            .resolve(AgentDeskRuntimeConfig.defaults())

                    val ready = screenState.shouldBeInstanceOf<DesktopScreenState.Ready>()
                    ready.modeLabel shouldBe "Sample state"
                    DesktopSmokeSnapshotBuilder.from(ready).flattenedText() shouldContain "Work state"
                }
            }

            `when`("a stored-event config is resolved") {
                then("it resolves a loaded screen state") {
                    val screenState =
                        DesktopStateResolver { OperatorState(workItems = emptyList(), events = emptyList()) }
                            .resolve(storedEventConfig())

                    val ready = screenState.shouldBeInstanceOf<DesktopScreenState.Ready>()
                    ready.modeLabel shouldBe "Loaded state"
                    DesktopSmokeSnapshotBuilder.from(ready).summary shouldBe "0 active / 0 attention"
                }
            }

            `when`("loading and error screen states are rendered") {
                then("their snapshot rows stay public-safe") {
                    val loading = DesktopSmokeSnapshotBuilder.from(DesktopScreenState.Loading)
                    val error =
                        DesktopSmokeSnapshotBuilder.from(
                            DesktopScreenState.Error("Configured event store could not be read."),
                        )

                    loading.sectionRows("Decision queue") shouldBe listOf("Loading operator state")
                    error.sectionRows("Decision queue") shouldBe
                        listOf("Configured event store could not be read.")
                    loading.sectionRows("Replay status") shouldBe
                        listOf("Replay status: loading operator state.")
                    error.sectionRows("Replay status") shouldBe
                        listOf("Blocked/error: Configured event store could not be read.")
                }
            }

            `when`("the loader fails") {
                then("it resolves a sanitized error state") {
                    val screenState =
                        DesktopStateResolver { error("boom with private details") }
                            .resolve(storedEventConfig())

                    val error = screenState.shouldBeInstanceOf<DesktopScreenState.Error>()
                    error.message shouldBe "Configured operator state could not be loaded."
                }
            }
        }
    })

private fun storedEventConfig(): AgentDeskRuntimeConfig = AgentDeskRuntimeConfig(
    mode = AgentDeskMode.StoredEvents,
    source = RuntimeEventSourceKind.LocalEventStore,
    eventStoreLocation = EventStoreLocation.parse("agent-desk-events.ndjson"),
)

private fun DesktopSmokeSnapshot.sectionRows(title: String): List<String> = sections.single { it.title == title }.rows
