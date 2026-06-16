package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.OperatorStateProjector
import com.yonatankarp.agentdesk.app.operator.SampleOperatorState
import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.ProvenanceId
import com.yonatankarp.agentdesk.core.domain.events.WorkProvenance
import com.yonatankarp.agentdesk.core.domain.projections.StaleWorkAttention
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary
import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import com.yonatankarp.agentdesk.testfixtures.matchers.shouldHaveNoActionAffordances
import com.yonatankarp.agentdesk.testfixtures.sanitizedNoteEvidence
import com.yonatankarp.agentdesk.testfixtures.workEvents
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class DesktopSmokeSnapshotTest :
    BehaviorSpec({
        given("the desktop smoke snapshot builder") {
            `when`("building from the sample operator state") {
                then("it exposes the expected desktop sections") {
                    val snapshot = DesktopSmokeSnapshotBuilder.from(SampleOperatorState.current())
                    val text = snapshot.flattenedText()

                    text shouldContain "Agent Desk"
                    text shouldContain "Sample state"
                    text shouldContain "Replay status"
                    text shouldContain "Work state"
                    text shouldContain "Read-only timeline"
                    text shouldContain "Decision queue"
                    text shouldContain "Evidence drilldown"
                    text shouldContain "Not done: 2 item(s) need operator attention."
                    text shouldContain "Discovery/no-issue output is triage, not product completion."
                    text shouldContain "[Running] agent-task:42 Run public hygiene check"
                    text shouldContain "Date: 2026-06-02"
                    text shouldContain "work.started at 2026-06-02T21:00:00Z"
                    text shouldContain "State: Read-only; Agent accepted the task and started local checks."
                    text shouldContain "[agent-task:42 from sample-agent]"
                    text shouldContain "[Needs decision] agent-task:43 Choose adapter boundary"
                    text shouldContain "[Blocked] agent-task:44 Review build failure"
                    text shouldContain "Observation: work.blocked for agent-task:44"
                    text shouldContain "Source: sample-agent"
                    text shouldContain "Provenance: replay event event:agent-task:44:blocked"
                    text shouldContain "Criteria result: Not done: 2 item(s) need operator attention."
                    text shouldContain "Evidence missing: no public-safe evidence reference was attached."
                    text shouldContain "Operator notes: unavailable in read-only proof."
                    text shouldContain "Redacted evidence: raw provider payloads are not rendered."
                    text shouldContain
                        "Diagnostics: raw provider data and arbitrary local file opening are unavailable by design."
                    text.shouldHaveNoActionAffordances()
                }

                then("it keeps the shared desktop section order") {
                    val snapshot = DesktopSmokeSnapshotBuilder.from(SampleOperatorState.current())

                    snapshot.sections.map { section -> section.title } shouldBe listOf(
                        "Replay status",
                        "Work state",
                        "Read-only timeline",
                        "Decision queue",
                        "Evidence drilldown",
                    )
                }
            }

            `when`("building from an empty operator state") {
                then("it exposes explicit empty rows") {
                    val snapshot =
                        DesktopSmokeSnapshotBuilder.from(
                            DesktopScreenState.Ready(
                                state = OperatorState(workItems = emptyList(), events = emptyList()),
                                modeLabel = "Loaded state",
                            ),
                        )

                    snapshot.modeLabel shouldBe "Loaded state"
                    snapshot.summary shouldBe "0 active / 0 attention"
                    snapshot.sectionRows("Replay status").joinToString("\n") shouldContain
                        "Empty queue: no current work or decisions; not product completion without milestone evidence."
                    snapshot.sectionRows("Work state") shouldBe listOf("No current work")
                    snapshot.sectionRows("Read-only timeline") shouldBe listOf("No recent events")
                    snapshot.sectionRows("Decision queue") shouldBe listOf("No items need a decision")
                    snapshot.sectionRows("Evidence drilldown") shouldBe listOf(
                        "Evidence missing: no replay events are available.",
                        "Criteria result: Empty queue: no current work or decisions; not product completion without milestone evidence.",
                        "Operator notes: unavailable in read-only proof.",
                        "Diagnostics: raw provider data and arbitrary local file opening are unavailable by design.",
                    )
                }
            }

            `when`("events include completed, failed, and stale entries across time windows") {
                then("desktop renders the shared read-only timeline projection fields") {
                    val state = timelineParityState()

                    val rows = DesktopSmokeSnapshotBuilder.from(
                        DesktopScreenState.Ready(state = state, modeLabel = "Loaded state"),
                    ).sectionRows("Read-only timeline")

                    rows.first() shouldBe "Status - Read-only, Stale, Failed, Completed [Read-only timeline projection]"
                    rows shouldContain "Date: 2026-06-02"
                    rows shouldContain "work.succeeded at 2026-06-02T21:10:00Z - State: Completed; Succeeded; " +
                        "Completion: Successful outcome; Evidence: sanitized-note Completion note -> " +
                        "docs/evidence/completed.md; Provenance: project:agent-desk workspace:local " +
                        "repo:agent-desk owner:local agent:ororo model:gpt-5 tool:gradle run:daily-20260616 " +
                        "objective:issue-238 handoff:parent-42 archive:event-42 [agent-task:42 from mock-adapter]"
                    rows shouldContain "work.failed at 2026-06-03T09:05:00Z - State: Failed; Build failed.; " +
                        "Completion: Failed outcome [agent-task:43 from mock-adapter]"
                    rows shouldContain "work.started at 2026-06-03T09:10:00Z - State: Stale; Waiting on background import.; " +
                        "Evidence: sanitized-note Stale note -> docs/evidence/stale.md [agent-task:44 from mock-adapter]"
                }
            }

            `when`("the latest event carries structured provenance") {
                then("desktop evidence drilldown renders the public-safe provenance fields") {
                    val provenance = publicSafeProvenance()
                    val state = OperatorStateProjector.project(
                        workEvents {
                            started(
                                workItemId = "agent-task:42",
                                title = "Ship provenance display",
                                summary = "Agent is preserving public-safe provenance.",
                                provenance = provenance,
                            )
                            blocked(
                                workItemId = "agent-task:42",
                                reason = "Waiting on dashboard filter review.",
                                provenance = provenance,
                            )
                        },
                    )

                    val rows = DesktopSmokeSnapshotBuilder.from(
                        DesktopScreenState.Ready(state = state, modeLabel = "Loaded state"),
                    ).sectionRows("Evidence drilldown")

                    rows shouldContain "Provenance: replay event event:agent-task:42:blocked"
                    rows shouldContain "Provenance fields: project:agent-desk workspace:local " +
                        "repo:agent-desk owner:local agent:ororo model:gpt-5 tool:gradle run:daily-20260616 " +
                        "objective:issue-238 handoff:parent-42 archive:event-42"
                    rows.joinToString("\n").shouldBePublicSafe()
                    rows.joinToString("\n").shouldHaveNoActionAffordances()
                }
            }

            `when`("an item needs operator attention") {
                then("it appears in the attention queue") {
                    val item =
                        WorkItem(
                            id = WorkItemId.parse("agent-task:91"),
                            title = WorkItemTitle.parse("Choose retry path"),
                            status = WorkStatus.NeedsDecision,
                            summary = WorkSummary.parse("Operator must choose whether to retry."),
                        )

                    val snapshot =
                        DesktopSmokeSnapshotBuilder.from(
                            DesktopScreenState.Ready(
                                state = OperatorState(workItems = listOf(item), events = emptyList()),
                                modeLabel = "Loaded state",
                            ),
                        )

                    snapshot.summary shouldBe "1 active / 1 attention"
                    snapshot.sectionRows("Decision queue") shouldBe listOf(
                        "[Needs decision] agent-task:91 Choose retry path - Operator must choose whether to retry.",
                    )
                }
            }

            `when`("current work has evidence and stale attention") {
                then("desktop work and attention rows render the parity metadata") {
                    val snapshot = DesktopSmokeSnapshotBuilder.from(
                        DesktopScreenState.Ready(
                            state = workAttentionParityState(),
                            modeLabel = "Loaded state",
                        ),
                    )

                    snapshot.summary shouldBe "1 active / 1 attention"
                    snapshot.sectionRows("Work state") shouldBe listOf(
                        "[Running] agent-task:91 Inspect stored projection - Agent is checking accepted events. | " +
                            "Evidence: sanitized-note Desktop evidence -> docs/evidence/desktop.md",
                    )
                    snapshot.sectionRows("Decision queue") shouldBe listOf(
                        "[Running] agent-task:91 Inspect stored projection - Agent is checking accepted events. | " +
                            "Evidence: sanitized-note Desktop evidence -> docs/evidence/desktop.md " +
                            "(Stale 1h 30m since 2026-06-02 21:00 UTC)",
                    )
                }
            }

            `when`("a work item reached a canceled terminal state") {
                then("the canceled label passes the action-verb denylist") {
                    val item =
                        WorkItem(
                            id = WorkItemId.parse("agent-task:50"),
                            title = WorkItemTitle.parse("Roll out the migration"),
                            status = WorkStatus.Canceled,
                            summary = WorkSummary.parse("Run reached a canceled terminal state."),
                        )

                    val text =
                        DesktopSmokeSnapshotBuilder.from(
                            DesktopScreenState.Ready(
                                state = OperatorState(workItems = listOf(item), events = emptyList()),
                                modeLabel = "Loaded state",
                            ),
                        ).flattenedText()

                    text shouldContain "[Canceled] agent-task:50"
                    text.shouldHaveNoActionAffordances()
                }
            }

            `when`("a real Cancel action affordance reaches the render") {
                then("the action-verb denylist trips") {
                    val item =
                        WorkItem(
                            id = WorkItemId.parse("agent-task:51"),
                            title = WorkItemTitle.parse("Cancel the deployment"),
                            status = WorkStatus.NeedsDecision,
                            summary = WorkSummary.parse("Pending operator action."),
                        )

                    val text =
                        DesktopSmokeSnapshotBuilder.from(
                            DesktopScreenState.Ready(
                                state = OperatorState(workItems = listOf(item), events = emptyList()),
                                modeLabel = "Loaded state",
                            ),
                        ).flattenedText()

                    shouldThrow<AssertionError> { text.shouldHaveNoActionAffordances() }
                }
            }

            `when`("inspecting the flattened sample text") {
                then("the snapshot remains public safe") {
                    val text = DesktopSmokeSnapshotBuilder.from(SampleOperatorState.current()).flattenedText()

                    text.shouldBePublicSafe()
                }
            }

            `when`("building from the loading state") {
                then("it explains unavailable evidence") {
                    val snapshot = DesktopSmokeSnapshotBuilder.from(DesktopScreenState.Loading)

                    snapshot.sectionRows("Evidence drilldown") shouldBe listOf(
                        "Evidence unavailable until operator state is loaded.",
                        "Diagnostics: raw provider data and arbitrary local file opening are unavailable by design.",
                    )
                }
            }
        }
    })

private fun DesktopSmokeSnapshot.sectionRows(title: String): List<String> = sections.single { it.title == title }.rows

private fun publicSafeProvenance(): WorkProvenance = WorkProvenance(
    projectId = ProvenanceId.parse("project:agent-desk"),
    workspaceId = ProvenanceId.parse("workspace:local"),
    sourceId = ProvenanceId.parse("repo:agent-desk"),
    ownerId = ProvenanceId.parse("owner:local"),
    agentId = ProvenanceId.parse("agent:ororo"),
    modelId = ProvenanceId.parse("model:gpt-5"),
    toolId = ProvenanceId.parse("tool:gradle"),
    runId = ProvenanceId.parse("run:daily-20260616"),
    objectiveId = ProvenanceId.parse("objective:issue-238"),
    parentHandoffId = ProvenanceId.parse("handoff:parent-42"),
    archiveRecordId = ProvenanceId.parse("archive:event-42"),
)

private fun timelineParityState(): OperatorState {
    val provenance = publicSafeProvenance()
    val events = workEvents {
        started(
            workItemId = "agent-task:42",
            title = "Ship read-only timeline",
            summary = "Timeline work started.",
            provenance = provenance,
        )
        succeeded(
            workItemId = "agent-task:42",
            evidence = listOf(sanitizedNoteEvidence("Completion note", "docs/evidence/completed.md")),
            provenance = provenance,
        )
        started(
            workItemId = "agent-task:43",
            at = EventTimestamp.parse("2026-06-03T09:00:00Z"),
            title = "Run failing projection",
            summary = "Projection failure analysis started.",
        )
        failed(workItemId = "agent-task:43", at = EventTimestamp.parse("2026-06-03T09:05:00Z"))
        started(
            workItemId = "agent-task:44",
            at = EventTimestamp.parse("2026-06-03T09:10:00Z"),
            title = "Watch import freshness",
            summary = "Waiting on background import.",
            evidence = listOf(sanitizedNoteEvidence("Stale note", "docs/evidence/stale.md")),
        )
    }
    val projected = OperatorStateProjector.project(events)
    return projected.copy(
        staleAttention = listOf(
            StaleWorkAttention(
                workItemId = WorkItemId.parse("agent-task:44"),
                status = WorkStatus.Running,
                lastEventAt = EventTimestamp.parse("2026-06-03T09:10:00Z"),
                staleForMinutes = 120,
            ),
        ),
    )
}

private fun workAttentionParityState(): OperatorState {
    val events = workEvents {
        started(
            workItemId = "agent-task:91",
            title = "Inspect stored projection",
            summary = "Agent is checking accepted events.",
            evidence = listOf(sanitizedNoteEvidence("Desktop evidence", "docs/evidence/desktop.md")),
        )
    }
    val projected = OperatorStateProjector.project(events)
    return projected.copy(
        staleAttention = listOf(
            StaleWorkAttention(
                workItemId = WorkItemId.parse("agent-task:91"),
                status = WorkStatus.Running,
                lastEventAt = EventTimestamp.parse("2026-06-02T21:00:00Z"),
                staleForMinutes = 90,
            ),
        ),
    )
}
