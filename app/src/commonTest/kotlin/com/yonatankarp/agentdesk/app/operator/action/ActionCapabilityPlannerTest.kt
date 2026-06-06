package com.yonatankarp.agentdesk.app.operator.action

import com.yonatankarp.agentdesk.app.fixtures.AppFixtures
import com.yonatankarp.agentdesk.app.operator.EvidenceLine
import com.yonatankarp.agentdesk.app.operator.OperatorActionIntent
import com.yonatankarp.agentdesk.app.operator.OperatorStateProjector
import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class ActionCapabilityPlannerTest :
    BehaviorSpec({
        given("a blocked work item") {
            val state = OperatorStateProjector.project(
                listOf(
                    AppFixtures.workStartedEvent(),
                    AppFixtures.workBlockedEvent(),
                ),
            )
            val item = state.workItems.single()

            `when`("inspect is proposed") {
                then("it is preview-only and read-only") {
                    val proposal = ActionCapabilityPlanner.propose(
                        item = item,
                        action = OperatorActionIntent.Inspect,
                        evidenceReferences = listOf(
                            EvidenceLine(
                                kind = "check-run",
                                label = "Gradle Build",
                                target = "https://github.com/yonatankarp/agent-desk/actions/runs/26937983933",
                            ),
                        ),
                    )

                    assertSoftly(proposal) {
                        target.workItemId shouldBe "agent-task:42"
                        capability.state shouldBe ActionCapabilityState.PreviewOnly
                        capability.disabledReason shouldBe null
                        riskClass shouldBe ActionRiskClass.ReadOnly
                        requiredConfirmation shouldBe ActionConfirmationRequirement.None
                        executionMode shouldBe ActionExecutionMode.ProposalOnly
                        evidenceReferences.map { it.label }.shouldContainExactly("Gradle Build")
                    }
                }
            }

            `when`("resume is proposed") {
                then("it requires local confirmation but still has no executor") {
                    val proposal = ActionCapabilityPlanner.propose(
                        item = item,
                        action = OperatorActionIntent.Resume,
                    )

                    assertSoftly(proposal) {
                        capability.state shouldBe ActionCapabilityState.RequiresConfirmation
                        capability.disabledReason shouldBe null
                        expectedEffect shouldContain "without touching external providers"
                        riskClass shouldBe ActionRiskClass.LocalPreview
                        requiredConfirmation shouldBe ActionConfirmationRequirement.ConfirmLocal
                        executionMode shouldBe ActionExecutionMode.ProposalOnly
                    }
                }
            }

            `when`("stop is proposed") {
                then("it is marked destructive and unavailable") {
                    val proposal = ActionCapabilityPlanner.propose(
                        item = item,
                        action = OperatorActionIntent.Stop,
                    )

                    assertSoftly(proposal) {
                        capability.state shouldBe ActionCapabilityState.DestructiveUnavailable
                        capability.disabledReason.orEmpty() shouldContain "unavailable"
                        expectedEffect shouldContain "No action is executed"
                        riskClass shouldBe ActionRiskClass.Destructive
                        requiredConfirmation shouldBe ActionConfirmationRequirement.ExplicitApproval
                        executionMode shouldBe ActionExecutionMode.ProposalOnly
                    }
                }
            }
        }

        given("a terminal work item") {
            `when`("resume is proposed") {
                then("it explains why the action is unavailable") {
                    val item = WorkItem(
                        id = WorkItemId.parse("agent-task:99"),
                        title = WorkItemTitle.parse("Already completed"),
                        status = WorkStatus.Succeeded,
                    )

                    val proposal = ActionCapabilityPlanner.propose(
                        item = item,
                        action = OperatorActionIntent.Resume,
                    )

                    proposal.capability.state shouldBe ActionCapabilityState.ReadOnlyUnavailable
                    proposal.capability.disabledReason.orEmpty() shouldContain "Resume is unavailable"
                    proposal.executionMode shouldBe ActionExecutionMode.ProposalOnly
                }
            }
        }

        given("external side-effect proposals") {
            `when`("an external send or account action is represented") {
                then("it stays unavailable and proposal-only") {
                    val target = ActionTarget(
                        workItemId = "agent-task:42",
                        title = "Review operator action",
                        status = "Blocked",
                    )

                    val proposal = ActionCapabilityPlanner.unsupportedExternalProposal(
                        target = target,
                        action = OperatorActionIntent.Inspect,
                        expectedEffect = "No external send, public post, purchase, account change, provider write, or delete is executed.",
                    )

                    assertSoftly(proposal) {
                        capability.state shouldBe ActionCapabilityState.ExternalSideEffectUnavailable
                        capability.disabledReason.orEmpty() shouldContain "External sends"
                        riskClass shouldBe ActionRiskClass.ExternalSideEffect
                        requiredConfirmation shouldBe ActionConfirmationRequirement.ExplicitApproval
                        executionMode shouldBe ActionExecutionMode.ProposalOnly
                    }
                }
            }
        }

        given("unsupported adapter actions") {
            `when`("an adapter cannot represent an action") {
                then("it returns an unsupported disabled proposal") {
                    val target = ActionTarget(
                        workItemId = "agent-task:42",
                        title = "Review operator action",
                        status = "Blocked",
                    )

                    val proposal = ActionCapabilityPlanner.unsupportedProposal(
                        target = target,
                        action = OperatorActionIntent.Stop,
                    )

                    assertSoftly(proposal) {
                        capability.state shouldBe ActionCapabilityState.Unsupported
                        capability.disabledReason.orEmpty() shouldContain "does not support"
                        expectedEffect shouldContain "No action is executed"
                        executionMode shouldBe ActionExecutionMode.ProposalOnly
                    }
                }
            }
        }

        given("proposal validation") {
            `when`("unsafe text is provided") {
                then("it rejects without echoing private details") {
                    val unsafeTitle = "Read " + "/" + "home/user/private.log"
                    val error = shouldThrow<IllegalArgumentException> {
                        ActionTarget(
                            workItemId = "agent-task:42",
                            title = unsafeTitle,
                            status = "Blocked",
                        )
                    }

                    error.message.orEmpty() shouldContain "Action target title"
                    error.message.orEmpty() shouldNotContain unsafeTitle
                }
            }

            `when`("a destructive proposal lacks explicit approval") {
                then("it is rejected") {
                    val error = shouldThrow<IllegalArgumentException> {
                        ActionProposal(
                            target = ActionTarget(
                                workItemId = "agent-task:42",
                                title = "Review operator action",
                                status = "Blocked",
                            ),
                            action = OperatorActionIntent.Stop,
                            capability = ActionCapability(
                                state = ActionCapabilityState.DestructiveUnavailable,
                                disabledReason = "Unavailable.",
                            ),
                            expectedEffect = "No action is executed.",
                            riskClass = ActionRiskClass.Destructive,
                            evidenceReferences = emptyList(),
                            requiredConfirmation = ActionConfirmationRequirement.ConfirmLocal,
                        )
                    }

                    error.message.orEmpty() shouldContain "Destructive action proposals require explicit approval"
                }
            }
        }
    })
