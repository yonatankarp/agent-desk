package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.SampleOperatorState
import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.events.EventSource
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.projections.StaleWorkAttention
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary
import com.yonatankarp.agentdesk.testfixtures.WorkEventFixtures
import com.yonatankarp.agentdesk.testfixtures.commitEvidence
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class OperatorConsoleRendererTest :
    BehaviorSpec({
        val renderer = OperatorConsoleRenderer()

        given("an operator state with work items, attention, and events") {
            `when`("the state is rendered") {
                then("it renders current work, attention queue, and event timeline") {
                    val staleWorkItemId = WorkItemId.parse("agent-task:45")
                    val output = renderer.render(
                        OperatorState(
                            workItems = listOf(
                                WorkEventFixtures.workItem(
                                    status = WorkStatus.NeedsDecision,
                                    summary = WorkSummary.parse("Operator decision needed before continuing."),
                                ),
                                WorkItem(
                                    id = staleWorkItemId,
                                    title = WorkItemTitle.parse("Watch long-running import"),
                                    status = WorkStatus.Running,
                                ),
                            ),
                            events = listOf(
                                WorkEventFixtures.workStartedEvent(
                                    source = EventSource.parse("sample-agent"),
                                    payload = WorkStartedPayload(
                                        title = WorkEventFixtures.workTitle,
                                        summary = WorkSummary.parse("Agent accepted the task and started checks."),
                                    ),
                                ).copy(
                                    evidenceReferences = listOf(
                                        commitEvidence(
                                            "Implementation commit",
                                            "commit:80de32988617392e1f42e6c4c48c66a56aaae4c4",
                                        ),
                                    ),
                                ),
                            ),
                            staleAttention = listOf(
                                StaleWorkAttention(
                                    workItemId = staleWorkItemId,
                                    status = WorkStatus.Running,
                                    lastEventAt = EventTimestamp.parse("2026-06-02T21:00:00Z"),
                                    staleForMinutes = 90,
                                ),
                            ),
                        ),
                    )

                    assertSoftly(output) {
                        shouldContain("Current work")
                        shouldContain("- [Needs decision] agent-task:42 Run public hygiene check")
                        shouldContain("Attention queue")
                        shouldContain("- agent-task:42 Run public hygiene check (Needs decision)")
                        shouldContain(
                            "- agent-task:45 Watch long-running import (Stale Running, last event 90m before latest event)",
                        )
                        shouldContain(
                            "- 2026-06-02T21:00:00Z work.started agent-task:42 from sample-agent - " +
                                "Agent accepted the task and started checks. | evidence: " +
                                "commit Implementation commit -> commit:80de32988617392e1f42e6c4c48c66a56aaae4c4",
                        )
                    }
                }
            }
        }

        given("an operator state with no work items or events") {
            `when`("the state is rendered") {
                then("it renders empty sections explicitly") {
                    val output = renderer.render(OperatorState(workItems = emptyList(), events = emptyList()))

                    output shouldBe
                        """
                        Agent Desk

                        Current work
                        - none

                        Attention queue
                        - none

                        Recent events
                        - none
                        """.trimIndent()
                }
            }
        }

        given("the bundled sample operator state") {
            `when`("the state is rendered") {
                then("the sample output stays public safe and adapter neutral") {
                    val output = renderer.render(SampleOperatorState.current())

                    assertSoftly(output) {
                        shouldContain("sample-agent")
                        shouldNotContain("/home/")
                        // adapter-neutral, public-safe: case-insensitive like the original assertions
                        lowercase() shouldNotContain "discord"
                        lowercase() shouldNotContain "token"
                        lowercase() shouldNotContain "op://"
                    }
                }
            }
        }
    })
