package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.core.domain.events.ProvenanceId
import com.yonatankarp.agentdesk.core.domain.events.WorkProvenance
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus
import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import com.yonatankarp.agentdesk.testfixtures.workEvents
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class OperatorStatePresenterTest :
    BehaviorSpec({
        given("sample operator state") {
            val state = SampleOperatorState.current()

            `when`("the presenter derives operational counts") {
                then("active and attention work are reusable across adapters") {
                    OperatorStatePresenter.activeCount(state) shouldBe 3
                    OperatorStatePresenter
                        .attentionItems(state)
                        .map { it.id.toString() }
                        .shouldContainExactly("agent-task:43", "agent-task:44")
                }
            }

            `when`("the presenter derives event lines") {
                then("lines remain public-safe and adapter-neutral") {
                    val text = OperatorStatePresenter.eventLines(state).joinToString("\n") { line ->
                        "${line.occurredAt} ${line.type} ${line.workItemId} ${line.source} ${line.detail}"
                    }

                    assertSoftly {
                        text shouldContain "sample-agent"
                        text shouldContain "work.started"
                        text shouldContain "work.blocked"
                        text.shouldBePublicSafe()
                    }
                }
            }
        }

        given("events with provenance") {
            `when`("the presenter derives event lines") {
                then("lines expose public-safe provenance for shared clients") {
                    val state = OperatorStateProjector.project(
                        workEvents {
                            started(
                                provenance = WorkProvenance(
                                    projectId = ProvenanceId.parse("project:agent-desk"),
                                    sourceId = ProvenanceId.parse("repo:agent-desk"),
                                    ownerId = ProvenanceId.parse("owner:local"),
                                    agentId = ProvenanceId.parse("agent:ororo"),
                                ),
                            )
                        },
                    )

                    val provenance = OperatorStatePresenter.eventLines(state).single().provenance!!

                    assertSoftly {
                        provenance.projectId shouldBe "project:agent-desk"
                        provenance.sourceId shouldBe "repo:agent-desk"
                        provenance.ownerId shouldBe "owner:local"
                        provenance.agentId shouldBe "agent:ororo"
                        provenance.toString().shouldBePublicSafe()
                    }
                }
            }
        }

        given("work status presentation") {
            `when`("attention statuses are presented") {
                then("labels stay separate from UI tone") {
                    OperatorStatePresenter.presentationFor(WorkStatus.NeedsDecision) shouldBe
                        StatusPresentation("Needs decision", StatusTone.Attention)
                    OperatorStatePresenter.presentationFor(WorkStatus.Blocked) shouldBe
                        StatusPresentation("Blocked", StatusTone.Blocked)
                }
            }
        }
    })
