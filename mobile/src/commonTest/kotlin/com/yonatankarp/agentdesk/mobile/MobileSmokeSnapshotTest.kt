package com.yonatankarp.agentdesk.mobile

import com.yonatankarp.agentdesk.app.operator.StatusTone
import com.yonatankarp.agentdesk.app.operator.mobile.MobileAttentionItem
import com.yonatankarp.agentdesk.app.operator.mobile.MobileEventLine
import com.yonatankarp.agentdesk.app.operator.mobile.MobileEvidenceDetail
import com.yonatankarp.agentdesk.app.operator.mobile.MobileEvidenceReference
import com.yonatankarp.agentdesk.app.operator.mobile.MobileOperatorState
import com.yonatankarp.agentdesk.app.operator.mobile.MobileProjectionWarning
import com.yonatankarp.agentdesk.app.operator.mobile.MobileStaleAttention
import com.yonatankarp.agentdesk.app.operator.mobile.MobileStatusPresentation
import com.yonatankarp.agentdesk.app.operator.mobile.MobileTimelineEntry
import com.yonatankarp.agentdesk.app.operator.mobile.MobileWorkItem
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class MobileSmokeSnapshotTest :
    FunSpec({
        test("sample snapshot shows read-only current work and attention queue") {
            val snapshot = MobileSmokeSnapshotBuilder.sample()
            val text = snapshot.flattenedText()

            snapshot.title shouldBe "Agent Desk"
            text shouldContain "3 current / 2 attention"
            text shouldContain "[Running] agent-task:42 Run public hygiene check"
            text shouldContain "[Needs decision] agent-task:43 Choose adapter boundary"
            text shouldContain "[Blocked] agent-task:44 Review build failure"
            text shouldContain "agent-task:42"
            text.shouldHaveNoActionAffordances()
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
                timeline = listOf(
                    MobileTimelineEntry(
                        eventId = "event:agent-task:91:started",
                        occurredAt = "2026-06-02T21:00:00Z",
                        timeWindow = "2026-06-02",
                        source = "mock-adapter",
                        workItemId = "agent-task:91",
                        type = "work.started",
                        statusLabel = "Running",
                        stateLabel = "Read-only",
                        summary = "Agent is checking accepted events.",
                        completionSummary = null,
                    ),
                ),
                timelineStatusMarkers = listOf("Read-only"),
                evidenceDetails = listOf(
                    MobileEvidenceDetail(
                        eventId = "event:agent-task:91:started",
                        source = "mock-adapter",
                        timestamp = "2026-06-02T21:00:00Z",
                        summary = "Agent is checking accepted events.",
                        provenance = "replay event event:agent-task:91:started",
                    ),
                ),
            )

            val text = MobileSmokeSnapshotBuilder.from(state).flattenedText()

            text shouldContain "Evidence: check-run Mobile smoke -> https://github.com/yonatankarp/agent-desk/actions/runs/26937983933"
            text shouldContain "Stale 1h 30m since 2026-06-02 21:00 UTC"
            text shouldContain "Recent events"
            text shouldContain "2026-06-02T21:03:00Z [Evidence attached] agent-task:91"
            text shouldContain "Accepted event includes mobile smoke evidence."
            text shouldContain "Projection warnings"
            text shouldContain "event:agent-task:91:blocked-after-success"
            text shouldContain "Status: Read-only"
            text shouldContain "2026-06-02T21:00:00Z [work.started] agent-task:91 from mock-adapter [Read-only]"
            text shouldContain "Provenance: replay event event:agent-task:91:started"
            text shouldContain "Related events: none"
            text shouldContain "Redacted evidence: raw provider payloads are not rendered."
            text.shouldHaveNoActionAffordances()
        }

        test("canceled-terminal timeline label passes the action-verb denylist") {
            val state = MobileOperatorState(
                currentWork = emptyList(),
                attentionQueue = emptyList(),
                recentEvents = emptyList(),
                timeline = listOf(
                    MobileTimelineEntry(
                        eventId = "event:agent-task:50:canceled",
                        occurredAt = "2026-06-02T21:10:00Z",
                        timeWindow = "2026-06-02",
                        source = "mock-adapter",
                        workItemId = "agent-task:50",
                        type = "work.canceled",
                        statusLabel = "Canceled",
                        stateLabel = "Read-only",
                        summary = "Run reached a canceled terminal state.",
                        completionSummary = "Canceled outcome",
                    ),
                ),
            )

            val text = MobileSmokeSnapshotBuilder.from(state).flattenedText()

            text shouldContain "(Canceled outcome)"
            text.shouldHaveNoActionAffordances()
        }

        test("a real Cancel action affordance still trips the denylist") {
            val state = MobileOperatorState(
                currentWork = listOf(
                    MobileWorkItem(
                        id = "agent-task:51",
                        title = "Cancel the deployment",
                        summary = "Pending operator action.",
                        status = MobileStatusPresentation(label = "Running", tone = StatusTone.Active),
                    ),
                ),
                attentionQueue = emptyList(),
                recentEvents = emptyList(),
            )

            val text = MobileSmokeSnapshotBuilder.from(state).flattenedText()

            shouldThrow<AssertionError> { text.shouldHaveNoActionAffordances() }
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
            snapshot.sectionRows("Timeline") shouldBe listOf("No timeline entries")
        }
    })

private fun MobileSmokeSnapshot.sectionRows(title: String): List<String> = sections.first { it.title == title }.rows

// Side-effecting action affordances must never render on the read-only mobile surface. Match each verb on
// word boundaries (not as a bare substring) so a completion label such as "Canceled outcome" or an audit
// label such as "Approved" does not false-positive while a real "Cancel"/"Approve" affordance still trips.
private val ACTION_VERBS = listOf("Resume", "Approve", "Stop", "Retry", "Cancel")

private fun String.shouldHaveNoActionAffordances() {
    ACTION_VERBS.forEach { verb ->
        withClue("read-only render must not expose the '$verb' action affordance") {
            "\\b$verb\\b".toRegex().containsMatchIn(this) shouldBe false
        }
    }
}
