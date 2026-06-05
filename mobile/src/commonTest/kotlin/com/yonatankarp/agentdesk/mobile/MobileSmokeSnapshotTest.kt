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
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MobileSmokeSnapshotTest {
    @Test
    fun sampleSnapshotShowsReadOnlyCurrentWorkAndAttentionQueue() {
        val snapshot = MobileSmokeSnapshotBuilder.sample()
        val text = snapshot.flattenedText()

        assertEquals("Agent Desk", snapshot.title)
        assertContains(text, "3 current / 2 attention")
        assertContains(text, "Current work")
        assertContains(text, "[Running] agent-task:42 Run public hygiene check")
        assertContains(text, "Attention queue")
        assertContains(text, "[Needs decision] agent-task:43 Choose adapter boundary")
        assertContains(text, "[Blocked] agent-task:44 Review build failure")
        assertContains(text, "Recent events")
        assertContains(text, "agent-task:42")
        assertFalse(text.contains("Resume"))
        assertFalse(text.contains("Approve"))
        assertFalse(text.contains("Stop"))
    }

    @Test
    fun snapshotIncludesStaleMarkersEvidenceReferencesAndProjectionWarnings() {
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

        assertContains(text, "Evidence: check-run: Mobile smoke")
        assertContains(text, "Stale 90m since 2026-06-02T21:00:00Z")
        assertContains(text, "Recent events")
        assertContains(text, "2026-06-02T21:03:00Z [Evidence attached] agent-task:91")
        assertContains(text, "Accepted event includes mobile smoke evidence.")
        assertContains(text, "Projection warnings")
        assertContains(text, "event:agent-task:91:blocked-after-success")
    }

    @Test
    fun emptySnapshotKeepsReadOnlySectionsVisible() {
        val snapshot = MobileSmokeSnapshotBuilder.from(
            MobileOperatorState(
                currentWork = emptyList(),
                attentionQueue = emptyList(),
                recentEvents = emptyList(),
            ),
        )

        assertEquals(listOf("No current work"), snapshot.sectionRows("Current work"))
        assertEquals(listOf("No items need attention"), snapshot.sectionRows("Attention queue"))
        assertEquals(listOf("No recent accepted events"), snapshot.sectionRows("Recent events"))
    }

    @Test
    fun mobileStatusToneColorsAreExhaustiveForOperatorTones() {
        assertEquals(MobilePalette.Accent, colorFor(StatusTone.Active))
        assertEquals(MobilePalette.Attention, colorFor(StatusTone.Attention))
        assertEquals(MobilePalette.Blocked, colorFor(StatusTone.Blocked))
        assertEquals(MobilePalette.Success, colorFor(StatusTone.Success))
        assertEquals(MobilePalette.Failure, colorFor(StatusTone.Failure))
    }

    private fun MobileSmokeSnapshot.sectionRows(title: String): List<String> = sections.first { it.title == title }.rows
}
