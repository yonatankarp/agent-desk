package com.yonatankarp.agentdesk.app.operator.timeline

import com.yonatankarp.agentdesk.app.fixtures.AppFixtures
import com.yonatankarp.agentdesk.app.fixtures.operatorState
import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.OperatorStateProjector
import com.yonatankarp.agentdesk.core.domain.events.EventSource
import com.yonatankarp.agentdesk.core.domain.events.ProvenanceId
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.events.WorkProvenance
import com.yonatankarp.agentdesk.core.domain.projections.StaleWorkAttention
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.testfixtures.sanitizedNoteEvidence
import com.yonatankarp.agentdesk.testfixtures.workEvents
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

class ReadOnlyTimelineProjectorTest :
    BehaviorSpec({
        given("projected operator state") {
            `when`("events include blocked and completed work") {
                then("it creates grouped read-only timeline entries") {
                    val started = AppFixtures.workStartedEvent(
                        id = WorkEventId.parse("event:agent-task:42:started"),
                        source = EventSource.parse("mock-adapter"),
                    )
                    val blocked = AppFixtures.workBlockedEvent(
                        id = WorkEventId.parse("event:agent-task:42:blocked"),
                        source = EventSource.parse("mock-adapter"),
                    )
                    val succeeded = AppFixtures.workSucceededEvent(
                        id = WorkEventId.parse("event:agent-task:43:succeeded"),
                        source = EventSource.parse("openclaw-local"),
                        workItemId = workItemId("agent-task:43"),
                    )
                    val succeededStart = AppFixtures.workStartedEvent(
                        id = WorkEventId.parse("event:agent-task:43:started"),
                        source = EventSource.parse("openclaw-local"),
                        workItemId = workItemId("agent-task:43"),
                    )
                    val state = OperatorStateProjector.project(
                        listOf(started, blocked, succeededStart, succeeded),
                    )

                    val projection = ReadOnlyTimelineProjector.project(state)

                    projection.entries.map { it.eventId }.shouldContainExactly(
                        "event:agent-task:42:started",
                        "event:agent-task:42:blocked",
                        "event:agent-task:43:started",
                        "event:agent-task:43:succeeded",
                    )
                    projection.sourceGroups.map { it.key }.shouldContainExactlyInAnyOrder(
                        "mock-adapter",
                        "openclaw-local",
                    )
                    projection.workItemGroups.map { it.key }.shouldContainExactlyInAnyOrder(
                        "agent-task:42",
                        "agent-task:43",
                    )
                    projection.timeWindowGroups.single().key shouldBe "2026-06-02"
                    projection.stateMarkers.shouldContain(ReadOnlyTimelineStateMarker.Blocked)
                    projection.stateMarkers.shouldContain(ReadOnlyTimelineStateMarker.Completed)
                    projection.entries.last().completionSummary shouldBe "Successful outcome"
                }
            }

            `when`("operator state is empty") {
                then("it reports empty read-only state") {
                    val projection = ReadOnlyTimelineProjector.project(
                        OperatorState(workItems = emptyList(), events = emptyList()),
                    )

                    projection.entries shouldBe emptyList()
                    projection.sourceGroups shouldBe emptyList()
                    projection.stateMarkers.shouldContainExactly(
                        ReadOnlyTimelineStateMarker.Empty,
                        ReadOnlyTimelineStateMarker.ReadOnly,
                    )
                }
            }

            `when`("events carry public-safe provenance") {
                then("it exposes query facets for project, source, owner, agent, status, risk, and recency") {
                    val projectA = WorkProvenance(
                        projectId = ProvenanceId.parse("project:agent-desk"),
                        workspaceId = ProvenanceId.parse("workspace:desktop"),
                        sourceId = ProvenanceId.parse("repo:agent-desk"),
                        ownerId = ProvenanceId.parse("owner:local"),
                        agentId = ProvenanceId.parse("agent:ororo"),
                        modelId = ProvenanceId.parse("model:gpt-5"),
                        toolId = ProvenanceId.parse("tool:gradle"),
                        runId = ProvenanceId.parse("run:daily-20260616"),
                    )
                    val projectB = WorkProvenance(
                        projectId = ProvenanceId.parse("project:archive"),
                        workspaceId = ProvenanceId.parse("workspace:mobile"),
                        sourceId = ProvenanceId.parse("repo:archive"),
                        ownerId = ProvenanceId.parse("owner:ops"),
                        agentId = ProvenanceId.parse("agent:relay"),
                    )
                    val state = operatorState {
                        started(provenance = projectA)
                        started(
                            workItemId = "agent-task:77",
                            title = "Review archived provenance",
                            summary = "Agent imported replay archive metadata.",
                            provenance = projectB,
                        )
                        failed(
                            workItemId = "agent-task:77",
                            reason = "Replay archive verification failed.",
                            provenance = projectB,
                        )
                    }

                    val projection = ReadOnlyTimelineProjector.project(state)

                    assertSoftly(projection.entries.first()) {
                        provenance.projectId shouldBe "project:agent-desk"
                        provenance.sourceId shouldBe "repo:agent-desk"
                        provenance.ownerId shouldBe "owner:local"
                        provenance.agentId shouldBe "agent:ororo"
                        provenance.modelId shouldBe "model:gpt-5"
                        provenance.toolId shouldBe "tool:gradle"
                    }
                    projection.projectGroups.map { it.key }.shouldContainExactlyInAnyOrder(
                        "project:agent-desk",
                        "project:archive",
                    )
                    projection.workspaceGroups.map { it.key }.shouldContainExactlyInAnyOrder(
                        "workspace:desktop",
                        "workspace:mobile",
                    )
                    projection.ownerGroups.map { it.key }.shouldContainExactlyInAnyOrder("owner:local", "owner:ops")
                    projection.agentGroups.map { it.key }.shouldContainExactlyInAnyOrder("agent:ororo", "agent:relay")
                    projection.sourceGroups.map { it.key }.shouldContainExactly("mock-adapter")
                    projection.upstreamSourceGroups.map { it.key }.shouldContainExactlyInAnyOrder(
                        "repo:agent-desk",
                        "repo:archive",
                    )
                    projection.statusGroups.map { it.key }.shouldContainExactlyInAnyOrder("Running", "Failed")
                    projection.riskGroups.map { it.key }.shouldContainExactlyInAnyOrder("ReadOnly", "Failed")
                    projection.timeWindowGroups.single().key shouldBe "2026-06-02"
                }

                then("it can apply project, source, owner, status, risk, and recency filters") {
                    val projectA = WorkProvenance(
                        projectId = ProvenanceId.parse("project:agent-desk"),
                        workspaceId = ProvenanceId.parse("workspace:desktop"),
                        sourceId = ProvenanceId.parse("repo:agent-desk"),
                        ownerId = ProvenanceId.parse("owner:local"),
                    )
                    val projectB = WorkProvenance(
                        projectId = ProvenanceId.parse("project:archive"),
                        workspaceId = ProvenanceId.parse("workspace:mobile"),
                        sourceId = ProvenanceId.parse("repo:archive"),
                        ownerId = ProvenanceId.parse("owner:ops"),
                    )
                    val state = operatorState {
                        started(provenance = projectA)
                        started(
                            workItemId = "agent-task:77",
                            title = "Review archived provenance",
                            summary = "Agent imported replay archive metadata.",
                            provenance = projectB,
                        )
                        failed(
                            workItemId = "agent-task:77",
                            reason = "Replay archive verification failed.",
                            provenance = projectB,
                        )
                    }

                    val projection = ReadOnlyTimelineProjector.project(
                        state,
                        filter = ReadOnlyTimelineFilter(
                            projectId = "project:archive",
                            workspaceId = "workspace:mobile",
                            upstreamSourceId = "repo:archive",
                            ownerId = "owner:ops",
                            status = "Failed",
                            risk = "Failed",
                            timeWindow = "2026-06-02",
                        ),
                    )

                    projection.entries.map { it.eventId }.shouldContainExactly(
                        "event:agent-task:77:started",
                        "event:agent-task:77:failed",
                    )
                    projection.projectGroups.single().key shouldBe "project:archive"
                    projection.workspaceGroups.single().key shouldBe "workspace:mobile"
                    projection.upstreamSourceGroups.single().key shouldBe "repo:archive"
                    projection.ownerGroups.single().key shouldBe "owner:ops"
                    projection.statusGroups.single().key shouldBe "Failed"
                    projection.riskGroups.single().key shouldBe "Failed"
                    projection.timeWindowGroups.single().key shouldBe "2026-06-02"
                }
            }

            `when`("state has stale attention and evidence") {
                then("entries carry stale, evidence, and diagnostic markers without private text") {
                    val events = workEvents {
                        started(evidence = listOf(sanitizedNoteEvidence("Replay evidence", "docs/canonical-sanitized-replay.md")))
                    }
                    val eventWithEvidence = events.single()
                    val projected = OperatorStateProjector.project(events)
                    val state = projected.copy(
                        staleAttention = listOf(
                            StaleWorkAttention(
                                workItemId = AppFixtures.workItemId,
                                status = projected.workItems.single().status,
                                lastEventAt = eventWithEvidence.occurredAt,
                                staleForMinutes = 120,
                            ),
                        ),
                    )

                    val projection = ReadOnlyTimelineProjector.project(state)
                    val entry = projection.entries.single()

                    assertSoftly {
                        entry.state shouldBe ReadOnlyTimelineEntryState.Stale
                        entry.evidenceReferences.single().target shouldBe "docs/canonical-sanitized-replay.md"
                        entry.diagnosticMarkers.shouldContainExactly("read-only", "import-diagnostics-from-replay")
                        projection.stateMarkers.shouldContain(ReadOnlyTimelineStateMarker.Stale)
                        projection.entries.joinToString("\n") { it.summary } shouldNotContain "/home/"
                        projection.entries.joinToString("\n") { it.summary } shouldNotContain "token"
                    }
                }
            }

            `when`("events include failed and partial entries") {
                then("it exposes failure and partial markers") {
                    val failed = operatorState {
                        started()
                        failed()
                    }
                    val partial = failed.copy(workItems = emptyList())

                    val failedProjection = ReadOnlyTimelineProjector.project(failed)
                    val partialProjection = ReadOnlyTimelineProjector.project(partial)

                    failedProjection.stateMarkers.shouldContain(ReadOnlyTimelineStateMarker.Failed)
                    failedProjection.entries.last().completionSummary shouldBe "Failed outcome"
                    partialProjection.stateMarkers.shouldContain(ReadOnlyTimelineStateMarker.Partial)
                    partialProjection.entries.first().status shouldBe "Partial"
                }
            }
        }
    })

private fun workItemId(raw: String): WorkItemId = WorkItemId.parse(raw)
