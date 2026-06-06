package com.yonatankarp.agentdesk.app.operator.timeline

import com.yonatankarp.agentdesk.app.fixtures.AppFixtures
import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.OperatorStateProjector
import com.yonatankarp.agentdesk.core.domain.events.EventSource
import com.yonatankarp.agentdesk.core.domain.events.EvidenceLabel
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReference
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReferenceKind
import com.yonatankarp.agentdesk.core.domain.events.EvidenceTarget
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.projections.StaleWorkAttention
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
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

            `when`("state has stale attention and evidence") {
                then("entries carry stale, evidence, and diagnostic markers without private text") {
                    val eventWithEvidence = AppFixtures.workStartedEvent().copy(
                        evidenceReferences = listOf(
                            EvidenceReference(
                                kind = EvidenceReferenceKind.SanitizedNote,
                                label = EvidenceLabel.parse("Replay evidence"),
                                target = EvidenceTarget.parse("docs/canonical-sanitized-replay.md"),
                            ),
                        ),
                    )
                    val projected = OperatorStateProjector.project(
                        listOf(eventWithEvidence),
                    )
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
                    val failed = OperatorStateProjector.project(
                        listOf(
                            AppFixtures.workStartedEvent(),
                            AppFixtures.workFailedEvent(),
                        ),
                    )
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
