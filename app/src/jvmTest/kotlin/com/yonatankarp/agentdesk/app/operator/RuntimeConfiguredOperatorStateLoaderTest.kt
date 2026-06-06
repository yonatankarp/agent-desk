package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.app.config.AgentDeskMode
import com.yonatankarp.agentdesk.app.config.AgentDeskRuntimeConfig
import com.yonatankarp.agentdesk.app.config.EventStoreLocation
import com.yonatankarp.agentdesk.app.config.RuntimeEventSourceKind
import com.yonatankarp.agentdesk.app.fixtures.AppFixtures.workBlockedEvent
import com.yonatankarp.agentdesk.app.fixtures.AppFixtures.workStartedEvent
import com.yonatankarp.agentdesk.app.persistence.LocalFileWorkEventRepository
import com.yonatankarp.agentdesk.app.serialization.WorkEventJson
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files
import java.nio.file.Path

class RuntimeConfiguredOperatorStateLoaderTest :
    BehaviorSpec({
        given("sample runtime configuration") {
            `when`("state is loaded") {
                then("it returns the shared sample operator state") {
                    val state = RuntimeConfiguredOperatorStateLoader.load(AgentDeskRuntimeConfig.defaults())

                    state.workItems.map { it.id.toString() } shouldContainExactly
                        SampleOperatorState.current().workItems.map { it.id.toString() }
                }
            }
        }

        given("stored event runtime configuration") {
            `when`("a local event store contains a valid event sequence") {
                then("it reads the repository and projects operator state") {
                    val storePath = tempStorePath()
                    val repository = LocalFileWorkEventRepository(storePath)
                    repository.append(workStartedEvent())
                    repository.append(workBlockedEvent())

                    val state = RuntimeConfiguredOperatorStateLoader.load(storedEventsConfig(storePath))

                    state.workItems.single().id.toString() shouldBe "agent-task:42"
                    state.workItems.single().status.name shouldBe "Blocked"
                    state.events.map { it.id.toString() }.shouldContainExactly(
                        "event:agent-task:42:started",
                        "event:agent-task:42:blocked",
                    )
                }
            }

            `when`("the configured event store has a torn trailing record") {
                then("it projects the committed prefix and carries a public-safe warning") {
                    val storePath = tempStorePath()
                    Files.writeString(
                        storePath,
                        WorkEventJson.encode(workStartedEvent()) + "\n" +
                            WorkEventJson.encode(workBlockedEvent()) + "\n" +
                            "{\"id\":\"event:agent-task:46:sta",
                    )

                    val state = RuntimeConfiguredOperatorStateLoader.load(storedEventsConfig(storePath))

                    state.events.map { it.id.toString() }.shouldContainExactly(
                        "event:agent-task:42:started",
                        "event:agent-task:42:blocked",
                    )
                    assertSoftly(state.storeReadWarning.orEmpty()) {
                        shouldContain("Torn trailing record at line 3")
                        shouldNotContain(storePath.toString())
                        shouldNotContain("/home/")
                    }
                }
            }

            `when`("the configured event store is corrupt") {
                then("it fails without echoing the filesystem path") {
                    val storePath = tempStorePath()
                    Files.writeString(storePath, "{not-json}\n")

                    val error = shouldThrow<RuntimeConfiguredOperatorStateLoadException> {
                        RuntimeConfiguredOperatorStateLoader.load(storedEventsConfig(storePath))
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Corrupt work event record at line 1")
                        shouldNotContain(storePath.toString())
                        shouldNotContain("/home/")
                    }
                }
            }

            `when`("the configured event store contains an unsafe event id") {
                then("it fails without echoing the raw event id") {
                    val storePath = tempStorePath()
                    val unsafeEventId = "event:private-token:started"
                    val unsafeEvent =
                        """{"id":"$unsafeEventId","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter",""" +
                            "\"workItemId\":\"agent-task:42\",\"type\":\"work.started\"," +
                            "\"payload\":{\"title\":\"Run public hygiene check\"}}"
                    Files.writeString(
                        storePath,
                        "$unsafeEvent\n",
                    )

                    val error = shouldThrow<RuntimeConfiguredOperatorStateLoadException> {
                        RuntimeConfiguredOperatorStateLoader.load(storedEventsConfig(storePath))
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Corrupt work event record at line 1 in configured event store")
                        shouldNotContain(unsafeEventId)
                        shouldNotContain(storePath.toString())
                    }
                }
            }

            `when`("the configured event store path is invalid") {
                then("it fails without echoing the raw configured value") {
                    val error = shouldThrow<RuntimeConfiguredOperatorStateLoadException> {
                        RuntimeConfiguredOperatorStateLoader.load(storedEventsConfig("events\u0000broken.ndjson"))
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Configured event store could not be read.")
                        shouldNotContain("broken.ndjson")
                        shouldNotContain("\u0000")
                    }
                }
            }

            `when`("the configured events cannot be projected") {
                then("it fails with a public-safe projection message") {
                    val storePath = tempStorePath()
                    Files.writeString(storePath, WorkEventJson.encode(workBlockedEvent()) + "\n")

                    val error = shouldThrow<RuntimeConfiguredOperatorStateLoadException> {
                        RuntimeConfiguredOperatorStateLoader.load(storedEventsConfig(storePath))
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Invalid event sequence")
                        shouldContain("existing started work item")
                        shouldNotContain(storePath.toString())
                    }
                }
            }
        }
    }) {
    companion object {
        private fun tempStorePath(): Path = Files.createTempDirectory("agent-desk-runtime-config-test")
            .resolve("agent-desk-events.ndjson")

        private fun storedEventsConfig(storePath: Path): AgentDeskRuntimeConfig = storedEventsConfig(storePath.toString())

        private fun storedEventsConfig(storeLocation: String): AgentDeskRuntimeConfig = AgentDeskRuntimeConfig(
            mode = AgentDeskMode.StoredEvents,
            source = RuntimeEventSourceKind.LocalEventStore,
            eventStoreLocation = EventStoreLocation.parse(storeLocation),
        )
    }
}
