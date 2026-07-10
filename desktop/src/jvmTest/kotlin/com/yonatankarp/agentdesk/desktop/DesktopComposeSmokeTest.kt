package com.yonatankarp.agentdesk.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.SampleOperatorState
import com.yonatankarp.agentdesk.design.theme.InMemoryThemeModeStore
import com.yonatankarp.agentdesk.design.theme.ThemeMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

// ExperimentalTestApi opt-in is tracked debt: see issue #279 — remove when
// Compose Multiplatform stabilizes the test API.
@OptIn(ExperimentalTestApi::class)
class DesktopComposeSmokeTest :
    FunSpec({
        test("sample state renders visible shell text") {
            runComposeUiTest {
                setContent {
                    AgentDeskApp(DesktopScreenState.Ready(SampleOperatorState.current(), modeLabel = "Sample state"))
                }

                onNodeWithText("Agent Desk").assertIsDisplayed()
                onNodeWithText("Sample state").assertIsDisplayed()
                onNodeWithText("Replay status").assertIsDisplayed()
                onNodeWithText("Settings").assertIsDisplayed()
                onNodeWithText("Work state").assertIsDisplayed()
                onNodeWithText("Read-only timeline").assertIsDisplayed()
                onNodeWithText("Decision queue").assertIsDisplayed()
                onNodeWithText("Evidence drilldown").assertIsDisplayed()
                onNodeWithText("Not done: 2 item(s) need operator attention.").assertIsDisplayed()
                onNodeWithText("Run public hygiene check").assertIsDisplayed()
                onAllNodesWithText("Choose adapter boundary").assertCountEquals(2)
                onAllNodesWithText("Review build failure").assertCountEquals(2)
                onNodeWithText("Observation: work.blocked for agent-task:44").assertIsDisplayed()
            }
        }

        test("settings renders as a separate page") {
            runComposeUiTest {
                setContent {
                    AgentDeskApp(DesktopScreenState.Ready(SampleOperatorState.current(), modeLabel = "Sample state"))
                }

                onNodeWithText("Platform detail panels: host-specific capabilities stay platform-scoped").assertDoesNotExist()
                onNodeWithText("Settings").performClick()
                onNodeWithText("Platform detail panels: host-specific capabilities stay platform-scoped").assertIsDisplayed()
                onNodeWithText("Theme: Auto").assertIsDisplayed()
                onNodeWithText("Work state").assertDoesNotExist()
            }
        }

        test("empty state renders visible empty rows") {
            runComposeUiTest {
                setContent {
                    AgentDeskApp(
                        DesktopScreenState.Ready(
                            state = OperatorState(workItems = emptyList(), events = emptyList()),
                            modeLabel = "Loaded state",
                        ),
                    )
                }

                onNodeWithText("0 active / 0 attention").assertIsDisplayed()
                onNodeWithText("Empty queue: no current work or decisions; not product completion without milestone evidence.").assertIsDisplayed()
                onNodeWithText("No current work").assertIsDisplayed()
                onNodeWithText("No recent events").assertIsDisplayed()
                onNodeWithText("No items need a decision").assertIsDisplayed()
                onNodeWithText("Evidence missing: no replay events are available.").assertIsDisplayed()
            }
        }

        test("error state renders public-safe message") {
            runComposeUiTest {
                setContent {
                    AgentDeskApp(DesktopScreenState.Error("Configured operator state could not be loaded."))
                }

                onNodeWithText("Invalid configuration").assertIsDisplayed()
                onNodeWithText("Configured operator state could not be loaded.").assertIsDisplayed()
            }
        }

        test("theme toggle cycles mode and persists to store") {
            val store = InMemoryThemeModeStore(ThemeMode.System)
            runComposeUiTest {
                setContent {
                    AgentDeskApp(
                        DesktopScreenState.Ready(
                            state = OperatorState(workItems = emptyList(), events = emptyList()),
                            modeLabel = "Toggle test",
                        ),
                        themeStore = store,
                    )
                }

                onNodeWithText("Settings").performClick()
                onNodeWithText("Theme: Auto").assertIsDisplayed()
                onNodeWithText("Theme: Auto").performClick()
                onNodeWithText("Theme: Light").assertIsDisplayed()
                store.load() shouldBe ThemeMode.Light
                onNodeWithText("Theme: Light").performClick()
                onNodeWithText("Theme: Dark").assertIsDisplayed()
                store.load() shouldBe ThemeMode.Dark
            }
        }

        test("OperatorState overload renders visible shell text") {
            runComposeUiTest {
                setContent {
                    AgentDeskApp(SampleOperatorState.current())
                }
                onNodeWithText("Agent Desk").assertIsDisplayed()
            }
        }
    })
