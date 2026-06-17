package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.app.fixtures.operatorState
import com.yonatankarp.agentdesk.testfixtures.eventTimestampAt
import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class OperatorHealthProjectorTest :
    BehaviorSpec({
        given("healthy replayed state") {
            `when`("health is projected") {
                then("it exposes counts, timestamps, backend status, and next action") {
                    val summary = OperatorHealthProjector.project(
                        state = operatorState {
                            started()
                            blocked()
                        },
                        sourceLabel = "stored events",
                    )

                    assertSoftly(summary) {
                        status shouldBe OperatorHealthStatus.Healthy
                        ingestion shouldBe "Replayed 2 event(s) into operator state."
                        source shouldBe "Source: stored events."
                        backend shouldBe "Backend: local replay state readable."
                        lastEvent shouldBe "Last event: 2026-06-02T21:05:00.123Z."
                        lastReplay shouldBe "Last replay: not recorded."
                        nextSafeAction shouldBe "Next safe action: continue monitoring the replay timeline."
                        diagnostics shouldBe emptyList()
                    }
                }
            }
        }

        given("empty replay state") {
            `when`("health is projected") {
                then("it distinguishes no work from a broken store") {
                    val summary = OperatorHealthProjector.project(OperatorState(workItems = emptyList(), events = emptyList()))

                    assertSoftly(summary) {
                        status shouldBe OperatorHealthStatus.Empty
                        ingestion shouldBe "No replay events are available."
                        lastEvent shouldBe "Last event: none."
                        lastReplay shouldBe "Last replay: not recorded."
                        nextSafeAction shouldContain "import sanitized observations"
                        backend shouldBe "Backend: local replay state readable."
                    }
                }
            }
        }

        given("delayed attention") {
            `when`("health is projected") {
                then("it marks the state delayed and keeps the latest replay event") {
                    val summary = OperatorHealthProjector.project(
                        operatorState {
                            started()
                            started(
                                workItemId = "agent-task:77",
                                at = eventTimestampAt(minute = 1, hour = 22),
                                title = "Refresh operator summary",
                                summary = "Agent started a later task.",
                            )
                        },
                    )

                    assertSoftly(summary) {
                        status shouldBe OperatorHealthStatus.Delayed
                        ingestion shouldBe "Replayed 2 event(s); 1 delayed attention item(s) need review."
                        lastEvent shouldBe "Last event: 2026-06-02T22:01:00Z."
                        nextSafeAction shouldContain "review delayed attention"
                    }
                }
            }
        }

        given("a partial local-store read") {
            `when`("health is projected") {
                then("it preserves only the public-safe recovery warning") {
                    val summary = OperatorHealthProjector.project(
                        operatorState {
                            started()
                        }.copy(
                            storeReadWarning = "Torn trailing record at line 2 in configured event store; recovered 1 committed event(s). Appending is blocked until the store is repaired.",
                        ),
                    )

                    assertSoftly(summary) {
                        status shouldBe OperatorHealthStatus.PartialImport
                        ingestion shouldBe "Recovered 1 committed event(s) from a partial import."
                        diagnostics.shouldContainExactly(
                            "Torn trailing record at line 2 in configured event store; recovered 1 committed event(s). Appending is blocked until the store is repaired.",
                        )
                        nextSafeAction shouldContain "repair the event store"
                        diagnostics.joinToString("\n").shouldBePublicSafe()
                    }
                }
            }
        }

        given("a failed runtime import") {
            `when`("health is projected from a public-safe error") {
                then("it gives a failed import diagnostic without raw source details") {
                    val summary = OperatorHealthProjector.failedImport("Configured event store could not be read.")

                    assertSoftly(summary) {
                        status shouldBe OperatorHealthStatus.FailedImport
                        ingestion shouldBe "Runtime import failed."
                        diagnostics.shouldContainExactly("Configured event store could not be read.")
                        diagnostics.joinToString("\n").shouldBePublicSafe()
                    }
                }
            }
        }

        given("source access failures") {
            `when`("the source is disconnected") {
                then("it exposes a source disconnected health state") {
                    val summary = OperatorHealthProjector.sourceDisconnected("Runtime source could not be reached.")

                    assertSoftly(summary) {
                        status shouldBe OperatorHealthStatus.SourceDisconnected
                        ingestion shouldBe "Runtime source is disconnected."
                        source shouldBe "Source: disconnected."
                        nextSafeAction shouldContain "reconnect the runtime source"
                        diagnostics.shouldContainExactly("Runtime source could not be reached.")
                    }
                }
            }

            `when`("source permission is missing") {
                then("it exposes a source permission missing health state") {
                    val summary = OperatorHealthProjector.sourcePermissionMissing("Runtime source permission is missing.")

                    assertSoftly(summary) {
                        status shouldBe OperatorHealthStatus.SourcePermissionMissing
                        ingestion shouldBe "Runtime source permission is missing."
                        source shouldBe "Source: permission missing."
                        nextSafeAction shouldContain "restore runtime source read permission"
                        diagnostics.shouldContainExactly("Runtime source permission is missing.")
                    }
                }
            }
        }
    })
