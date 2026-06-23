package com.yonatankarp.agentdesk.mobile

import com.yonatankarp.agentdesk.app.serialization.WorkEventJson
import com.yonatankarp.agentdesk.core.domain.events.EventSource
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary
import com.yonatankarp.agentdesk.testfixtures.WorkEventFixtures
import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.writeText

class MobileRuntimeStateProviderTest :
    BehaviorSpec({
        given("the mobile runtime state provider") {
            `when`("loading without args") {
                then("it keeps the public-safe sample state") {
                    val state = MobileRuntimeStateProvider.load(emptyArray())

                    state.currentWork.size shouldBe 3
                    state.attentionQueue.size shouldBe 2
                    state.projectionWarnings shouldBe emptyList()
                }
            }

            `when`("loading from a public-safe stored-event config") {
                then("it renders configured operator state through the mobile contract") {
                    val directory = Files.createTempDirectory(testRoot(), "agent-desk-mobile-config-test")
                    val eventStore = directory.resolve("events.ndjson")
                    val config = directory.resolve("agent-desk.config.properties")
                    eventStore.writeText(WorkEventJson.encode(startedEvent()) + "\n")
                    writeStoredEventsConfig(config, eventStore)

                    val state = MobileRuntimeStateProvider.load(arrayOf("--config", config.toString()))
                    val text = MobileSmokeSnapshotBuilder.from(state).flattenedText()

                    state.currentWork.map { item -> item.id } shouldContainExactly listOf("agent-task:386")
                    text shouldContain "Loaded from sanitized mobile store"
                    text.shouldBePublicSafe()
                    text shouldNotContain eventStore.toString()
                }
            }

            `when`("the configured store has a torn trailing record") {
                then("it shows a public projection warning") {
                    val directory = Files.createTempDirectory(testRoot(), "agent-desk-mobile-torn-test")
                    val eventStore = directory.resolve("events.ndjson")
                    val config = directory.resolve("agent-desk.config.properties")
                    eventStore.writeText(WorkEventJson.encode(startedEvent()) + "\n{\"id\":\"event:agent-task:386:sta")
                    writeStoredEventsConfig(config, eventStore)

                    val state = MobileRuntimeStateProvider.load(arrayOf("--config", config.toString()))
                    val warning = state.projectionWarnings.single()

                    warning.eventId shouldBe "configured-event-store"
                    warning.reason shouldContain "Torn trailing record at line 2 in configured event store"
                    warning.reason.shouldBePublicSafe()
                    warning.reason shouldNotContain eventStore.toString()
                }
            }

            `when`("loading with invalid args") {
                then("it shows a public-safe usage warning") {
                    val state = MobileRuntimeStateProvider.load(arrayOf("--events", "agent-desk-events.ndjson"))

                    state.currentWork shouldBe emptyList()
                    state.projectionWarnings.single().reason shouldBe
                        "Usage: agent-desk-mobile [--config <properties-file>]"
                }
            }

            `when`("the config file cannot be read") {
                then("it shows a public-safe missing config warning") {
                    val rawPath = "missing-agent-desk-mobile-config.properties"

                    val state = MobileRuntimeStateProvider.load(arrayOf("--config", rawPath))
                    val warning = state.projectionWarnings.single()

                    warning.reason shouldBe "config file could not be read"
                    warning.reason.shouldBePublicSafe()
                    warning.reason shouldNotContain rawPath
                }
            }

            `when`("the configured store cannot be projected") {
                then("it shows a public-safe load failure") {
                    val directory = Files.createTempDirectory(testRoot(), "agent-desk-mobile-corrupt-test")
                    val eventStore = directory.resolve("events.ndjson")
                    val config = directory.resolve("agent-desk.config.properties")
                    eventStore.writeText("""{"id":"event:agent-task:386:started","type":"work.started"}""" + "\n")
                    writeStoredEventsConfig(config, eventStore)

                    val state = MobileRuntimeStateProvider.load(arrayOf("--config", config.toString()))
                    val warning = state.projectionWarnings.single()

                    warning.eventId shouldBe "mobile-config"
                    warning.reason shouldContain "configured event store"
                    warning.reason.shouldBePublicSafe()
                    warning.reason shouldNotContain eventStore.toString()
                }
            }
        }
    })

private fun startedEvent() = WorkEventFixtures.workStartedEvent(
    id = WorkEventId.parse("event:agent-task:386:started"),
    source = EventSource.parse("test-store"),
    workItemId = WorkItemId.parse("agent-task:386"),
    payload = WorkStartedPayload(
        title = WorkItemTitle.parse("Loaded from sanitized mobile store"),
        summary = WorkSummary.parse("Mobile loaded projected operator state."),
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
    val root = Path.of("build", "tmp", "mobile-runtime-state-provider-test")
    Files.createDirectories(root)
    return root
}
