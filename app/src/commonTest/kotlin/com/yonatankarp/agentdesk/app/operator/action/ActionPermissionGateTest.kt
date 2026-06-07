package com.yonatankarp.agentdesk.app.operator.action

import com.yonatankarp.agentdesk.app.fixtures.projectedWorkItem
import com.yonatankarp.agentdesk.app.operator.Actor
import com.yonatankarp.agentdesk.app.operator.OperatorActionIntent
import com.yonatankarp.agentdesk.app.operator.audit.AuditResult
import com.yonatankarp.agentdesk.app.operator.audit.AuditTrailProjector
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class ActionPermissionGateTest :
    BehaviorSpec({
        given("read-only permission") {
            `when`("a read-only proposal is evaluated") {
                then("it is approved without approval and logged publicly") {
                    val decision = ActionPermissionGate.decide(
                        request(
                            proposal = inspectProposal(),
                            actionClass = PermissionedActionClass.ReadOnly,
                            intentSummary = "Inspect sanitized evidence.",
                        ),
                    )

                    val entry = AuditTrailProjector.fromPermissionDecision(decision)

                    assertSoftly(decision) {
                        state shouldBe PermissionDecisionState.Approved
                        behavior shouldBe PermissionGateBehavior.AllowWithoutApproval
                        logSummary shouldContain "without external side effects"
                    }
                    assertSoftly(entry) {
                        result shouldBe AuditResult.Approved
                        action shouldBe "permission.readonly"
                        detail shouldContain "without external side effects"
                    }
                }
            }
        }

        given("local write permission") {
            `when`("approval is missing") {
                then("it is denied before any action can proceed") {
                    val decision = ActionPermissionGate.decide(
                        request(
                            proposal = resumeProposal(),
                            actionClass = PermissionedActionClass.LocalWrite,
                            intentSummary = "Resume local mock work.",
                        ),
                    )

                    decision.state shouldBe PermissionDecisionState.Denied
                    decision.behavior shouldBe PermissionGateBehavior.RequireLocalConfirmation
                    decision.logSummary shouldContain "approval is required"
                }
            }

            `when`("operator approves") {
                then("it records a local-only approval") {
                    val decision = ActionPermissionGate.decide(
                        request(
                            proposal = resumeProposal(),
                            actionClass = PermissionedActionClass.LocalWrite,
                            intentSummary = "Resume local mock work.",
                            approval = approval(PermissionApprovalOutcome.Approve),
                        ),
                    )

                    decision.state shouldBe PermissionDecisionState.Approved
                    decision.logSummary shouldContain "local proposal loop"
                }
            }

            `when`("operator cancels") {
                then("it records cancellation and denies execution") {
                    val decision = ActionPermissionGate.decide(
                        request(
                            proposal = resumeProposal(),
                            actionClass = PermissionedActionClass.LocalWrite,
                            intentSummary = "Resume local mock work.",
                            approval = approval(PermissionApprovalOutcome.Cancel),
                        ),
                    )

                    decision.state shouldBe PermissionDecisionState.Canceled
                    decision.logSummary shouldContain "canceled"
                }
            }

            `when`("operator rejects") {
                then("it records rejection and denies execution") {
                    val decision = ActionPermissionGate.decide(
                        request(
                            proposal = resumeProposal(),
                            actionClass = PermissionedActionClass.LocalWrite,
                            intentSummary = "Resume local mock work.",
                            approval = approval(PermissionApprovalOutcome.Reject),
                        ),
                    )

                    decision.state shouldBe PermissionDecisionState.Denied
                    decision.behavior shouldBe PermissionGateBehavior.RequireLocalConfirmation
                    decision.logSummary shouldContain "rejected by operator"
                }
            }
        }

        given("external and ambiguous permissions") {
            `when`("an external-send action is unavailable") {
                then("approval cannot bypass the disabled proposal") {
                    val decision = ActionPermissionGate.decide(
                        request(
                            proposal = externalProposal(),
                            actionClass = PermissionedActionClass.ExternalSend,
                            intentSummary = "Send a public-safe status update.",
                            approval = approval(PermissionApprovalOutcome.Approve),
                        ),
                    )

                    decision.state shouldBe PermissionDecisionState.Denied
                    decision.behavior shouldBe PermissionGateBehavior.DenyUnavailable
                    decision.logSummary shouldContain "proposal-only model"
                }
            }

            `when`("intent is ambiguous") {
                then("it requires clarification and fails closed") {
                    val decision = ActionPermissionGate.decide(
                        request(
                            proposal = resumeProposal(),
                            actionClass = PermissionedActionClass.LocalWrite,
                            intentSummary = "Maybe resume or stop the item.",
                            ambiguous = true,
                        ),
                    )

                    decision.state shouldBe PermissionDecisionState.RequiresClarification
                    decision.behavior shouldBe PermissionGateBehavior.RequireClarification
                    decision.logSummary shouldContain "no action is allowed"
                }
            }

            `when`("an unsupported adapter action is requested") {
                then("it is recorded as unsupported") {
                    val decision = ActionPermissionGate.decide(
                        request(
                            proposal = unsupportedProposal(),
                            actionClass = PermissionedActionClass.Destructive,
                            intentSummary = "Stop unsupported work.",
                            approval = approval(PermissionApprovalOutcome.Approve),
                        ),
                    )

                    decision.state shouldBe PermissionDecisionState.Unsupported
                    decision.behavior shouldBe PermissionGateBehavior.DenyUnavailable
                }
            }
        }

        given("approval attribution") {
            `when`("a different operator approves a local write") {
                then("the decision adopts the approval actor and timestamp, not the request's") {
                    val decision = ActionPermissionGate.decide(
                        request(
                            proposal = resumeProposal(),
                            actionClass = PermissionedActionClass.LocalWrite,
                            intentSummary = "Resume local mock work.",
                            approval = ActionPermissionApproval(
                                outcome = PermissionApprovalOutcome.Approve,
                                actor = Actor.parse("operator:reviewer"),
                                decidedAt = EventTimestamp.parse("2026-06-06T09:30:00Z"),
                                rationale = "Reviewed the public-safe proposal.",
                            ),
                        ),
                    )

                    assertSoftly(decision) {
                        state shouldBe PermissionDecisionState.Approved
                        actor shouldBe Actor.parse("operator:reviewer")
                        decidedAt shouldBe EventTimestamp.parse("2026-06-06T09:30:00Z")
                    }
                }
            }

            `when`("intent is ambiguous even though an approval is attached") {
                then("ambiguity wins and the gate fails closed") {
                    val decision = ActionPermissionGate.decide(
                        request(
                            proposal = resumeProposal(),
                            actionClass = PermissionedActionClass.LocalWrite,
                            intentSummary = "Maybe resume the item.",
                            ambiguous = true,
                            approval = approval(PermissionApprovalOutcome.Approve),
                        ),
                    )

                    decision.state shouldBe PermissionDecisionState.RequiresClarification
                    decision.behavior shouldBe PermissionGateBehavior.RequireClarification
                }
            }
        }

        given("explicit approval classes on an enabled proposal") {
            `when`("a destructive-class request is approved") {
                then("it is approved for planning only with the explicit-approval behavior") {
                    val decision = ActionPermissionGate.decide(
                        request(
                            proposal = resumeProposal(),
                            actionClass = PermissionedActionClass.Destructive,
                            intentSummary = "Plan a destructive control.",
                            approval = approval(PermissionApprovalOutcome.Approve),
                        ),
                    )

                    decision.state shouldBe PermissionDecisionState.Approved
                    decision.behavior shouldBe PermissionGateBehavior.RequireExplicitApproval
                    decision.logSummary shouldContain "planning only"
                }
            }

            `when`("a destructive-class request is rejected") {
                then("it is denied with the rejection recorded") {
                    val decision = ActionPermissionGate.decide(
                        request(
                            proposal = resumeProposal(),
                            actionClass = PermissionedActionClass.Destructive,
                            intentSummary = "Plan a destructive control.",
                            approval = approval(PermissionApprovalOutcome.Reject),
                        ),
                    )

                    decision.state shouldBe PermissionDecisionState.Denied
                    decision.behavior shouldBe PermissionGateBehavior.RequireExplicitApproval
                    decision.logSummary shouldContain "rejected by operator"
                }
            }
        }

        given("public-safe validation") {
            `when`("private content is used in a loggable field") {
                then("it rejects without echoing the private value") {
                    val unsafeSummary = "Read " + "/" + "home/user/private.log"
                    val error = shouldThrow<IllegalArgumentException> {
                        request(
                            proposal = inspectProposal(),
                            actionClass = PermissionedActionClass.ReadOnly,
                            intentSummary = unsafeSummary,
                        )
                    }

                    error.message.orEmpty() shouldContain "Permission intent summary"
                    error.message.orEmpty() shouldNotContain unsafeSummary
                }
            }
        }
    })

private fun request(
    proposal: ActionProposal,
    actionClass: PermissionedActionClass,
    intentSummary: String,
    ambiguous: Boolean = false,
    approval: ActionPermissionApproval? = null,
): ActionPermissionRequest = ActionPermissionRequest(
    proposal = proposal,
    actionClass = actionClass,
    actor = Actor.parse("operator"),
    requestedAt = EventTimestamp.parse("2026-06-06T08:00:00Z"),
    intentSummary = intentSummary,
    ambiguous = ambiguous,
    approval = approval,
)

private fun approval(outcome: PermissionApprovalOutcome): ActionPermissionApproval = ActionPermissionApproval(
    outcome = outcome,
    actor = Actor.parse("operator"),
    decidedAt = EventTimestamp.parse("2026-06-06T08:01:00Z"),
    rationale = "Operator chose a public-safe permission outcome.",
)

private fun inspectProposal(): ActionProposal = ActionCapabilityPlanner.propose(
    item = blockedItem(),
    action = OperatorActionIntent.Inspect,
)

private fun resumeProposal(): ActionProposal = ActionCapabilityPlanner.propose(
    item = blockedItem(),
    action = OperatorActionIntent.Resume,
)

private fun externalProposal(): ActionProposal = ActionCapabilityPlanner.unsupportedExternalProposal(
    target = blockedTarget(),
    action = OperatorActionIntent.Inspect,
    expectedEffect = "No external send is executed.",
)

private fun unsupportedProposal(): ActionProposal = ActionCapabilityPlanner.unsupportedProposal(
    target = blockedTarget(),
    action = OperatorActionIntent.Stop,
)

private fun blockedTarget(): ActionTarget = blockedItem().let { item ->
    ActionTarget(
        workItemId = item.id,
        title = item.title.toString(),
        status = item.status.name,
    )
}

private fun blockedItem() = projectedWorkItem {
    started()
    blocked()
}
