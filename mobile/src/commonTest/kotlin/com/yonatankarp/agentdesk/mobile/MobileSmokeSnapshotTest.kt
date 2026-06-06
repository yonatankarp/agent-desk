package com.yonatankarp.agentdesk.mobile

import com.yonatankarp.agentdesk.app.operator.StatusTone
import com.yonatankarp.agentdesk.app.operator.mobile.MobileAttentionItem
import com.yonatankarp.agentdesk.app.operator.mobile.MobileEventLine
import com.yonatankarp.agentdesk.app.operator.mobile.MobileEvidenceReference
import com.yonatankarp.agentdesk.app.operator.mobile.MobileOperatorState
import com.yonatankarp.agentdesk.app.operator.mobile.MobileProjectionWarning
import com.yonatankarp.agentdesk.app.operator.mobile.MobileStaleAttention
import com.yonatankarp.agentdesk.app.operator.mobile.MobileStatusPresentation
import com.yonatankarp.agentdesk.app.operator.mobile.MobileWorkItem
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class MobileSmokeSnapshotTest :
    FunSpec({
        test("sample snapshot shows read-only current work and attention queue") {
            val snapshot = MobileSmokeSnapshotBuilder.sample()
            val text = snapshot.flattenedText()

            snapshot.title shouldBe "Agent Desk"
            text shouldContain "3 current / 2 attention"
            text shouldContain "Current work"
            text shouldContain "[Running] agent-task:42 Run public hygiene check"
            text shouldContain "Attention queue"
            text shouldContain "[Needs decision] agent-task:43 Choose adapter boundary"
            text shouldContain "[Blocked] agent-task:44 Review build failure"
            text shouldContain "Recent events"
            text shouldContain "agent-task:42"
            text shouldNotContain "Resume"
            text shouldNotContain "Approve"
            text shouldNotContain "Stop"
        }

        test("snapshot includes stale markers evidence references and projection warnings") {
            val state = MobileOperatorState(
                currentWork = listOf(
                    MobileWorkItem(
                        id = "agent-task:91",
                        title = "Inspect stored projection",
                        summary = "Agent is checking accepted events.",
                        status = MobileStatusPresentation(label = "Running", tone = StatusTone.Active),
                        evidenceReferences = listOf(
                            MobileEvidenceReference(
                                kind = "check-run",
                                label = "Mobile smoke",
                                target = "https://github.com/yonatankarp/agent-desk/actions/runs/26937983933",
                            ),
                        ),
                    ),
                ),
                attentionQueue = listOf(
                    MobileAttentionItem(
                        workItem = MobileWorkItem(
                            id = "agent-task:91",
                            title = "Inspect stored projection",
                            summary = "Agent is checking accepted events.",
                            status = MobileStatusPresentation(label = "Running", tone = StatusTone.Active),
                            evidenceReferences = listOf(
                                MobileEvidenceReference(
                                    kind = "check-run",
                                    label = "Mobile smoke",
                                    target = "https://github.com/yonatankarp/agent-desk/actions/runs/26937983933",
                                ),
                            ),
                        ),
                        reason = "Agent is checking accepted events.",
                        stale = MobileStaleAttention(
                            lastEventAt = "2026-06-02T21:00:00Z",
                            staleForMinutes = 90,
                        ),
                    ),
                ),
                recentEvents = listOf(
                    MobileEventLine(
                        occurredAt = "2026-06-02T21:03:00Z",
                        type = "Evidence attached",
                        workItemId = "agent-task:91",
                        detail = "Accepted event includes mobile smoke evidence.",
                        evidenceReferences = listOf(
                            MobileEvidenceReference(
                                kind = "check-run",
                                label = "Mobile smoke",
                                target = "https://github.com/yonatankarp/agent-desk/actions/runs/26937983933",
                            ),
                        ),
                    ),
                ),
                projectionWarnings = listOf(
                    MobileProjectionWarning(
                        eventId = "event:agent-task:91:blocked-after-success",
                        reason = "Cannot transition work item agent-task:91 from Succeeded to Blocked",
                    ),
                ),
            )

            val text = MobileSmokeSnapshotBuilder.from(state).flattenedText()

            text shouldContain "Evidence: check-run: Mobile smoke"
            text shouldContain "Stale 90m since 2026-06-02T21:00:00Z"
            text shouldContain "Recent events"
            text shouldContain "2026-06-02T21:03:00Z [Evidence attached] agent-task:91"
            text shouldContain "Accepted event includes mobile smoke evidence."
            text shouldContain "Projection warnings"
            text shouldContain "event:agent-task:91:blocked-after-success"
        }

        test("empty snapshot keeps read-only sections visible") {
            val snapshot = MobileSmokeSnapshotBuilder.from(
                MobileOperatorState(
                    currentWork = emptyList(),
                    attentionQueue = emptyList(),
                    recentEvents = emptyList(),
                ),
            )

            snapshot.sectionRows("Current work") shouldBe listOf("No current work")
            snapshot.sectionRows("Attention queue") shouldBe listOf("No items need attention")
            snapshot.sectionRows("Recent events") shouldBe listOf("No recent accepted events")
        }

        test("mobile status tone colors are exhaustive for operator tones") {
            colorFor(StatusTone.Active) shouldBe MobilePalette.Accent
            colorFor(StatusTone.Attention) shouldBe MobilePalette.Attention
            colorFor(StatusTone.Blocked) shouldBe MobilePalette.Blocked
            colorFor(StatusTone.Success) shouldBe MobilePalette.Success
            colorFor(StatusTone.Failure) shouldBe MobilePalette.Failure
        }
    })

private fun MobileSmokeSnapshot.sectionRows(title: String): List<String> = sections.first { it.title == title }.rows
