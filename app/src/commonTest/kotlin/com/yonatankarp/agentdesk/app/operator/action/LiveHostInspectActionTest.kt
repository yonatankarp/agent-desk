package com.yonatankarp.agentdesk.app.operator.action

import com.yonatankarp.agentdesk.app.fixtures.InMemoryWorkEventRepository
import com.yonatankarp.agentdesk.app.operator.Actor
import com.yonatankarp.agentdesk.app.operator.audit.AuditEntry
import com.yonatankarp.agentdesk.app.operator.audit.AuditResult
import com.yonatankarp.agentdesk.app.operator.audit.AuditTrailProjector
import com.yonatankarp.agentdesk.app.operator.audit.AuditTrailRecorder
import com.yonatankarp.agentdesk.app.persistence.AuditRecordReadResult
import com.yonatankarp.agentdesk.app.persistence.AuditRecordRepository
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostAccessBoundary
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostAlias
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostAuthState
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostPermissionMode
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.testfixtures.eventTimestampAt
import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import com.yonatankarp.agentdesk.testfixtures.sanitizedNoteEvidence
import com.yonatankarp.agentdesk.testfixtures.workEvents
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

class LiveHostInspectActionTest :
    BehaviorSpec({
        given("a configured action-capable host and stored operator state") {
            `when`("inspect is proposed without approval") {
                then("it records the proposal and denial without calling the adapter") {
                    val harness = harness()

                    val outcome = harness.inspect(approval = LiveHostInspectApproval.Deny)

                    val denied = outcome.shouldBeInstanceOf<LiveHostInspectOutcome.NotExecuted>()
                    assertSoftly {
                        denied.reason shouldContain "denied"
                        harness.adapter.calls shouldBe 0
                        harness.auditRepository.appended.map { it.action }.shouldContainExactly(
                            "live-inspect.proposal.created",
                            "live-inspect.approval.denied",
                        )
                        harness.auditRepository.appended.map { it.result }.shouldContainExactly(
                            AuditResult.Deferred,
                            AuditResult.Rejected,
                        )
                    }
                }
            }

            `when`("inspect is approved") {
                then("it calls the adapter once and records public-safe audit evidence") {
                    val harness = harness()

                    val outcome = harness.inspect(approval = harness.approval())

                    val executed = outcome.shouldBeInstanceOf<LiveHostInspectOutcome.Executed>()
                    assertSoftly {
                        harness.adapter.calls shouldBe 1
                        executed.output.summary shouldBe "Sanitized host status is blocked for agent-task:42."
                        executed.output.summary.shouldBePublicSafe()
                        executed.auditEntries.map { it.action }.shouldContainExactly(
                            "live-inspect.proposal.created",
                            "live-inspect.approval.approved",
                            "live-inspect.adapter.started",
                            "live-inspect.adapter.succeeded",
                            "live-inspect.output.rendered",
                        )
                        AuditTrailProjector.timelineLines(executed.auditEntries).forEach { line ->
                            line.detail.shouldBePublicSafe()
                            line.detail shouldNotContain privatePath()
                        }
                    }
                }
            }

            `when`("the adapter fails") {
                then("it records a public-safe adapter failure without rendering output") {
                    val harness = harness(adapter = RecordingLiveHostInspectAdapter(LiveHostInspectAdapterResult.Failed("Synthetic host timeout.")))

                    val outcome = harness.inspect(approval = harness.approval())

                    val failed = outcome.shouldBeInstanceOf<LiveHostInspectOutcome.NotExecuted>()
                    assertSoftly {
                        failed.reason shouldBe "Synthetic host timeout."
                        failed.auditEntries.map { it.action }.shouldContainExactly(
                            "live-inspect.proposal.created",
                            "live-inspect.approval.approved",
                            "live-inspect.adapter.started",
                            "live-inspect.adapter.failed",
                        )
                        failed.auditEntries.last().result shouldBe AuditResult.Failed
                    }
                }
            }

            `when`("approval does not match the exact proposal") {
                then("it fails closed before the adapter is called") {
                    val harness = harness()

                    val outcome = harness.inspect(
                        approval = LiveHostInspectApproval.Approve(
                            proposalId = "inspect:agent-task:other:2026-06-02T21:25:00Z",
                            hostAlias = RuntimeHostAlias.parse("host:primary"),
                            target = WorkItemId.parse("agent-task:42"),
                        ),
                    )

                    val notExecuted = outcome.shouldBeInstanceOf<LiveHostInspectOutcome.NotExecuted>()
                    assertSoftly {
                        notExecuted.reason shouldContain "exact proposal"
                        harness.adapter.calls shouldBe 0
                        notExecuted.auditEntries.map { it.action }.shouldContainExactly(
                            "live-inspect.proposal.created",
                            "live-inspect.approval.mismatched",
                        )
                    }
                }
            }

            `when`("the adapter returns unsafe output") {
                then("it rejects the output before rendering and does not echo the private payload") {
                    val unsafeSummary = "Read " + privatePath()
                    val harness = harness(
                        adapter = RecordingLiveHostInspectAdapter(
                            LiveHostInspectAdapterResult.UnsafeOutput("Inspect output contained private content."),
                        ),
                    )

                    val outcome = harness.inspect(approval = harness.approval())

                    val rejected = outcome.shouldBeInstanceOf<LiveHostInspectOutcome.NotExecuted>()
                    assertSoftly {
                        rejected.reason shouldBe "Inspect output contained private content."
                        rejected.reason shouldNotContain privatePath()
                        rejected.auditEntries.map { it.action }.shouldContainExactly(
                            "live-inspect.proposal.created",
                            "live-inspect.approval.approved",
                            "live-inspect.adapter.started",
                            "live-inspect.adapter.unsafe-rejected",
                        )
                        rejected.auditEntries.last().detail shouldNotContain unsafeSummary
                    }
                }
            }
        }
    })

private class LiveInspectRecordingAuditRepository : AuditRecordRepository {
    val appended = mutableListOf<AuditEntry>()

    override fun append(entry: AuditEntry) {
        appended += entry
    }

    override fun readAll(): AuditRecordReadResult = AuditRecordReadResult(entries = appended.toList())
}

private class RecordingLiveHostInspectAdapter(
    private val result: LiveHostInspectAdapterResult = LiveHostInspectAdapterResult.Succeeded(
        LiveHostInspectOutput(
            summary = "Sanitized host status is blocked for agent-task:42.",
            evidence = listOf("sanitized-note:host-inspect"),
        ),
    ),
) : LiveHostInspectAdapter {
    var calls: Int = 0
        private set

    override fun inspect(request: LiveHostInspectAdapterRequest): LiveHostInspectAdapterResult {
        calls += 1
        return result
    }
}

private class LiveInspectHarness(
    val auditRepository: LiveInspectRecordingAuditRepository,
    val adapter: RecordingLiveHostInspectAdapter,
    private val useCase: LiveHostInspectAction,
) {
    fun inspect(approval: LiveHostInspectApproval): LiveHostInspectOutcome = useCase.inspect(
        workItemId = WorkItemId.parse("agent-task:42"),
        actor = Actor.parse("operator:cli"),
        approval = approval,
        now = eventTimestampAt(minute = 25),
    )

    fun approval(): LiveHostInspectApproval = LiveHostInspectApproval.Approve(
        proposalId = LiveHostInspectProposal.idFor(
            target = WorkItemId.parse("agent-task:42"),
            createdAt = eventTimestampAt(minute = 25),
        ),
        hostAlias = RuntimeHostAlias.parse("host:primary"),
        target = WorkItemId.parse("agent-task:42"),
    )
}

private fun harness(
    adapter: RecordingLiveHostInspectAdapter = RecordingLiveHostInspectAdapter(),
): LiveInspectHarness {
    val eventRepository = InMemoryWorkEventRepository()
    workEvents {
        started()
        blocked(evidence = listOf(sanitizedNoteEvidence("Blocked context", "docs/blocked-context.md")))
    }.forEach(eventRepository::append)
    val auditRepository = LiveInspectRecordingAuditRepository()
    return LiveInspectHarness(
        auditRepository = auditRepository,
        adapter = adapter,
        useCase = LiveHostInspectAction(
            eventRepository = eventRepository,
            auditRecorder = AuditTrailRecorder(auditRepository),
            hostBoundary = RuntimeHostAccessBoundary(
                alias = RuntimeHostAlias.parse("host:primary"),
                authState = RuntimeHostAuthState.Accepted,
                permissionMode = RuntimeHostPermissionMode.ActionCapable,
            ),
            adapter = adapter,
            expiresAt = EventTimestamp.parse("2026-06-02T21:35:00Z"),
        ),
    )
}

private fun privatePath(): String = "/" + listOf("home", "operator", "agent", "desk.log").joinToString("/")
