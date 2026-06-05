package com.yonatankarp.agentdesk.mobile

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.yonatankarp.agentdesk.app.operator.mobile.MobileAttentionItem
import com.yonatankarp.agentdesk.app.operator.mobile.MobileEventLine
import com.yonatankarp.agentdesk.app.operator.mobile.MobileEvidenceReference
import com.yonatankarp.agentdesk.app.operator.mobile.MobileOperatorState
import com.yonatankarp.agentdesk.app.operator.mobile.MobileOperatorStateContract
import com.yonatankarp.agentdesk.app.operator.mobile.MobileProjectionWarning
import com.yonatankarp.agentdesk.app.operator.mobile.MobileStaleAttention
import kotlin.test.Test

class MobileComposeSmokeTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun sampleStateRendersVisibleShellText() = runComposeUiTest {
        setContent {
            AgentDeskMobileApp(MobileOperatorStateContract.sample())
        }

        onNodeWithText("Agent Desk").assertIsDisplayed()
        onNodeWithText("3 current / 2 attention").assertIsDisplayed()
        onNodeWithText("Current work").assertIsDisplayed()
        onNodeWithText("Attention queue").assertIsDisplayed()
        onNodeWithText("Run public hygiene check").assertIsDisplayed()
        onAllNodesWithText("Choose adapter boundary").assertCountEquals(2)
        onAllNodesWithText("Review build failure").assertCountEquals(2)
        onNodeWithText("Recent events").performScrollTo().assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun emptyStateRendersVisibleEmptyRows() = runComposeUiTest {
        setContent {
            AgentDeskMobileApp(
                MobileOperatorState(
                    currentWork = emptyList(),
                    attentionQueue = emptyList(),
                    recentEvents = emptyList(),
                ),
            )
        }

        onNodeWithText("0 current / 0 attention").assertIsDisplayed()
        onNodeWithText("No current work").assertIsDisplayed()
        onNodeWithText("No items need attention").assertIsDisplayed()
        onNodeWithText("No recent accepted events").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun staleEvidenceAndWarningsRenderVisibleText() = runComposeUiTest {
        val evidence = MobileEvidenceReference(
            kind = "check-run",
            label = "Mobile smoke",
            target = "https://github.com/yonatankarp/agent-desk/actions/runs/26937983933",
        )
        val workItem = MobileOperatorStateContract.sample().currentWork.first()

        setContent {
            AgentDeskMobileApp(
                MobileOperatorState(
                    currentWork = listOf(workItem.copy(evidenceReferences = listOf(evidence))),
                    attentionQueue = listOf(
                        MobileAttentionItem(
                            workItem = workItem.copy(evidenceReferences = listOf(evidence)),
                            reason = workItem.summary,
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
                            workItemId = workItem.id,
                            detail = "Accepted event includes mobile smoke evidence.",
                            evidenceReferences = listOf(evidence),
                        ),
                    ),
                    projectionWarnings = listOf(
                        MobileProjectionWarning(
                            eventId = "event:agent-task:91:blocked-after-success",
                            reason = "Cannot transition work item agent-task:91 from Succeeded to Blocked",
                        ),
                    ),
                ),
            )
        }

        onAllNodesWithText("check-run: Mobile smoke").assertCountEquals(3)
        onNodeWithText("Stale 90m since 2026-06-02T21:00:00Z").performScrollTo().assertIsDisplayed()
        onNodeWithText("Evidence attached").performScrollTo().assertIsDisplayed()
        onNodeWithText("Projection warnings").performScrollTo().assertIsDisplayed()
        onNodeWithText("Cannot transition work item agent-task:91 from Succeeded to Blocked")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
