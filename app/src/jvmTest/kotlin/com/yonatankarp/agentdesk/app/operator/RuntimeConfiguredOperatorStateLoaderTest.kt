package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.app.config.AgentDeskMode
import com.yonatankarp.agentdesk.app.config.AgentDeskRuntimeConfig
import com.yonatankarp.agentdesk.app.config.EventStoreLocation
import com.yonatankarp.agentdesk.app.config.RuntimeEventSourceKind
import com.yonatankarp.agentdesk.app.fixtures.AppFixtures.workBlockedEvent
import com.yonatankarp.agentdesk.app.fixtures.AppFixtures.workStartedEvent
import com.yonatankarp.agentdesk.app.persistence.LocalFileWorkEventRepository
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files
import java.nio.file.Path

class RuntimeConfiguredOperatorStateLoaderTest :
    BehaviorSpec({
        given("sample runtime configuration") {
            `when`("operator state is loaded") {
                then("it returns the public-safe sample state") {
                    val state = RuntimeConfiguredOperatorStateLoader().load(AgentDeskRuntimeConfig.defaults())

                    state.workItems.shouldHaveSize(3)
                    state.events.shouldHaveSize(5)
                }
            }
        }

        given("stored event runtime configuration") {
            `when`("the configured store has canonical records") {
                then("it reads and projects the store") {
                    val storePath = tempStorePath()
                    val repository = LocalFileWorkEventRepository(storePath)
                    repository.append(workStartedEvent())
                    repository.append(workBlockedEvent())

                    val state = RuntimeConfiguredOperatorStateLoader().load(storedConfig(storePath))

                    state.workItems.single().id.toString() shouldBe "agent-task:42"
                    state.events.shouldHaveSize(2)
                }
            }

            `when`("the configured store location cannot be used as a path") {
                then("it fails without echoing the raw value") {
                    val config = AgentDeskRuntimeConfig(
                        mode = AgentDeskMode.StoredEvents,
                        source = RuntimeEventSourceKind.LocalEventStore,
                        eventStoreLocation = EventStoreLocation.parse("bad\u0000store.ndjson"),
                    )

                    val error = shouldThrow<OperatorStateLoadException> {
                        RuntimeConfiguredOperatorStateLoader().load(config)
                    }

                    error.message shouldBe "Configured event store could not be read."
                }
            }

            `when`("the configured store is corrupt") {
                then("it preserves the public-safe store error") {
                    val storePath = tempStorePath()
                    Files.writeString(storePath, "{not-json}\n")

                    val error = shouldThrow<OperatorStateLoadException> {
                        RuntimeConfiguredOperatorStateLoader().load(storedConfig(storePath))
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Corrupt work event record at line 1")
                        shouldNotContain(storePath.toString())
                    }
                }
            }
        }
    }) {
    companion object {
        private fun tempStorePath(): Path = Files.createTempDirectory("agent-desk-config-loader-test")
            .resolve("events.ndjson")

        private fun storedConfig(storePath: Path): AgentDeskRuntimeConfig = AgentDeskRuntimeConfig(
            mode = AgentDeskMode.StoredEvents,
            source = RuntimeEventSourceKind.LocalEventStore,
            eventStoreLocation = EventStoreLocation.parse(storePath.toString()),
        )
    }
}
