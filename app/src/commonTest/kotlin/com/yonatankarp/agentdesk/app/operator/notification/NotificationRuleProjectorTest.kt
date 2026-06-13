package com.yonatankarp.agentdesk.app.operator.notification

import com.yonatankarp.agentdesk.app.fixtures.operatorState
import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.events.WorkNeedsDecisionPayload
import com.yonatankarp.agentdesk.core.domain.projections.StaleWorkAttention
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary
import com.yonatankarp.agentdesk.testfixtures.WorkEventFixtures
import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import com.yonatankarp.agentdesk.testfixtures.sanitizedNoteEvidence
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class NotificationRuleProjectorTest :
    BehaviorSpec({
        given("operator state that needs attention") {
            `when`("decision, blocked, failed, completed, and material-change work are present") {
                then("it derives public-safe signals grouped for the operator digest") {
                    val state = operatorState {
                        started(workItemId = "agent-task:decision")
                        needsDecision(workItemId = "agent-task:decision")
                        started(workItemId = "agent-task:blocked")
                        blocked(workItemId = "agent-task:blocked")
                        started(workItemId = "agent-task:failed")
                        failed(workItemId = "agent-task:failed")
                        started(workItemId = "agent-task:completed")
                        succeeded(workItemId = "agent-task:completed")
                        started(
                            workItemId = "agent-task:changed",
                            evidence = listOf(sanitizedNoteEvidence("Change summary", "docs/evidence/change.md")),
                        )
                    }

                    val projection = NotificationRuleProjector.project(state)

                    assertSoftly {
                        projection.deliveryMode shouldBe NotificationDeliveryMode.ReadOnly
                        projection.signals.map { it.rule }.shouldContainExactlyInAnyOrder(
                            NotificationRule.DecisionRequested,
                            NotificationRule.WorkBlocked,
                            NotificationRule.WorkFailed,
                            NotificationRule.WorkCompleted,
                            NotificationRule.MaterialChange,
                        )
                        projection.digestGroups.map { it.group }.shouldContainExactlyInAnyOrder(
                            DigestGroup.PendingDecisions,
                            DigestGroup.NewBlockers,
                            DigestGroup.CompletedWork,
                            DigestGroup.MaterialChanges,
                        )
                        projection.digestGroups
                            .single { it.group == DigestGroup.NewBlockers }
                            .signals
                            .map { it.rule }
                            .shouldContainExactlyInAnyOrder(NotificationRule.WorkBlocked, NotificationRule.WorkFailed)
                        projection.signals
                            .single { it.rule == NotificationRule.DecisionRequested }
                            .dedupeKey shouldBe "decision-requested:agent-task:decision"
                        projection.signals
                            .single { it.rule == NotificationRule.MaterialChange }
                            .reason shouldBe "New public-safe evidence is available for active work."
                        projection.signals.joinToString("\n") { signal ->
                            "${signal.title} ${signal.reason} ${signal.dedupeKey} ${signal.evidenceReferences.joinToString { it.target }}"
                        }.shouldBePublicSafe()
                    }
                }
            }

            `when`("quiet mode is requested") {
                then("it keeps the digest inspectable but suppresses normal and low immediate candidates") {
                    val state = operatorState {
                        started(
                            workItemId = "agent-task:changed",
                            evidence = listOf(sanitizedNoteEvidence("Change summary", "docs/evidence/change.md")),
                        )
                        started(workItemId = "agent-task:failed")
                        failed(workItemId = "agent-task:failed")
                    }

                    val projection = NotificationRuleProjector.project(state, deliveryMode = NotificationDeliveryMode.Quiet)

                    assertSoftly {
                        projection.signals.map { it.rule }.shouldContainExactlyInAnyOrder(
                            NotificationRule.MaterialChange,
                            NotificationRule.WorkFailed,
                        )
                        projection.immediateCandidates.map { it.rule }.shouldContainExactly(NotificationRule.WorkFailed)
                    }
                }
            }

            `when`("a digest window is provided") {
                then("it groups only signals recorded inside the window") {
                    val state = operatorState {
                        started(workItemId = "agent-task:decision")
                        needsDecision(workItemId = "agent-task:decision")
                        started(workItemId = "agent-task:blocked")
                        blocked(workItemId = "agent-task:blocked")
                        started(workItemId = "agent-task:failed")
                        failed(workItemId = "agent-task:failed")
                        started(workItemId = "agent-task:completed")
                        succeeded(workItemId = "agent-task:completed")
                    }

                    val projection = NotificationRuleProjector.project(
                        state = state,
                        window = DigestWindow(
                            startsAt = EventTimestamp.parse("2026-06-02T21:05:00Z"),
                            endsAt = EventTimestamp.parse("2026-06-02T21:10:00Z"),
                        ),
                    )

                    assertSoftly {
                        projection.signals.map { it.rule }.shouldContainExactlyInAnyOrder(
                            NotificationRule.WorkBlocked,
                            NotificationRule.WorkFailed,
                            NotificationRule.WorkCompleted,
                        )
                        projection.digestGroups.map { it.group }.shouldContainExactlyInAnyOrder(
                            DigestGroup.NewBlockers,
                            DigestGroup.CompletedWork,
                        )
                    }
                }
            }
        }

        given("stale attention") {
            `when`("running work is stale") {
                then("it adds one stale-work material-change signal") {
                    val running = operatorState {
                        started(workItemId = "agent-task:stale")
                    }
                    val state = OperatorState(
                        workItems = running.workItems,
                        events = running.events,
                        staleAttention = listOf(
                            StaleWorkAttention(
                                workItemId = WorkItemId.parse("agent-task:stale"),
                                status = WorkStatus.Running,
                                lastEventAt = EventTimestamp.parse("2026-06-02T21:00:00Z"),
                                staleForMinutes = 90,
                            ),
                        ),
                    )

                    val signal = NotificationRuleProjector.project(state).signals.single()

                    assertSoftly {
                        signal.rule shouldBe NotificationRule.WorkStale
                        signal.urgency shouldBe NotificationUrgency.Normal
                        signal.digestGroup shouldBe DigestGroup.MaterialChanges
                        signal.reason shouldBe "Running work has had no accepted event for 90 minutes."
                    }
                }
            }
        }

        given("flapping or duplicate observations") {
            `when`("the same rule appears more than once for one work item") {
                then("it keeps the latest signal for the stable dedupe key") {
                    val state = operatorState {
                        started(workItemId = "agent-task:decision")
                        needsDecision(
                            workItemId = "agent-task:decision",
                            at = EventTimestamp.parse("2026-06-02T21:03:00Z"),
                            reason = "Operator decision needed.",
                        )
                        event(
                            WorkEventFixtures.workNeedsDecisionEvent(
                                id = WorkEventId.parse("event:agent-task:decision:needs-decision-refresh"),
                                occurredAt = EventTimestamp.parse("2026-06-02T21:04:00Z"),
                                workItemId = WorkItemId.parse("agent-task:decision"),
                                payload = WorkNeedsDecisionPayload(
                                    reason = WorkSummary.parse("Operator decision still needed."),
                                ),
                            ),
                        )
                    }

                    val signals = NotificationRuleProjector.project(state).signals

                    assertSoftly {
                        signals.single().dedupeKey shouldBe "decision-requested:agent-task:decision"
                        signals.single().recordedAt shouldBe EventTimestamp.parse("2026-06-02T21:04:00Z")
                        signals.single().occurrenceCount shouldBe 2
                    }
                }
            }
        }
    })
