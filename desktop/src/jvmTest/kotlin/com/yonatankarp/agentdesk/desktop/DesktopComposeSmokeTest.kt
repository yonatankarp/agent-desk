package com.yonatankarp.agentdesk.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.SampleOperatorState
import kotlin.test.Test

class DesktopComposeSmokeTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `sample state renders visible shell text`() = runComposeUiTest {
        setContent {
            AgentDeskApp(DesktopScreenState.Ready(SampleOperatorState.current(), modeLabel = "Sample state"))
        }

        onNodeWithText("Agent Desk").assertIsDisplayed()
        onNodeWithText("Sample state").assertIsDisplayed()
        onNodeWithText("Current work").assertIsDisplayed()
        onNodeWithText("Recent events").assertIsDisplayed()
        onNodeWithText("Attention queue").assertIsDisplayed()
        onNodeWithText("Run public hygiene check").assertIsDisplayed()
        onAllNodesWithText("Choose adapter boundary").assertCountEquals(2)
        onAllNodesWithText("Review build failure").assertCountEquals(2)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `empty state renders visible empty rows`() = runComposeUiTest {
        setContent {
            AgentDeskApp(
                DesktopScreenState.Ready(
                    state = OperatorState(workItems = emptyList(), events = emptyList()),
                    modeLabel = "Loaded state",
                ),
            )
        }

        onNodeWithText("0 active / 0 attention").assertIsDisplayed()
        onNodeWithText("No current work").assertIsDisplayed()
        onNodeWithText("No recent events").assertIsDisplayed()
        onNodeWithText("No items need a decision").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `error state renders public-safe message`() = runComposeUiTest {
        setContent {
            AgentDeskApp(DesktopScreenState.Error("Configured operator state could not be loaded."))
        }

        onNodeWithText("Invalid configuration").assertIsDisplayed()
        onNodeWithText("Configured operator state could not be loaded.").assertIsDisplayed()
    }
}
