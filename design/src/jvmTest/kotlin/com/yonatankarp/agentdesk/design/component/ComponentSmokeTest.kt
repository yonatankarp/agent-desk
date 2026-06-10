package com.yonatankarp.agentdesk.design.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.font.FontFamily
import com.yonatankarp.agentdesk.app.operator.StatusTone
import com.yonatankarp.agentdesk.design.theme.AgentDeskTheme
import com.yonatankarp.agentdesk.design.theme.ThemeMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

// Test-only opt-in, tracked debt #279.
@OptIn(ExperimentalTestApi::class)
class ComponentSmokeTest :
    FunSpec({
        listOf(ThemeMode.Light, ThemeMode.Dark).forEach { mode ->
            test("design components render in $mode") {
                runComposeUiTest {
                    setContent {
                        AgentDeskTheme(mode = mode) {
                            Column {
                                Panel(title = "Work state", count = 1) {
                                    ActionRow(
                                        id = "AD-101",
                                        title = "Refactor auth flow",
                                        tone = StatusTone.Active,
                                        statusLabel = "Active",
                                    )
                                }
                                EventRow(
                                    type = "Commit pushed",
                                    occurredAt = "14:02",
                                    detail = "Pushed a3f9c",
                                    source = "AD-101 from openclaw",
                                )
                                EvidenceItem(label = "logs/run-AD101.txt")
                                SummaryChip(value = "3", label = "active")
                                ThemeModeControl(mode = mode, onCycle = {})
                            }
                        }
                    }
                    onNodeWithText("Work state").assertIsDisplayed()
                    onNodeWithText("AD-101").assertIsDisplayed()
                    onNodeWithText("Refactor auth flow").assertIsDisplayed()
                    onNodeWithText("Active").assertIsDisplayed()
                    onNodeWithText("Commit pushed").assertIsDisplayed()
                    onNodeWithText("logs/run-AD101.txt").assertIsDisplayed()
                }
            }
        }

        // Branch-coverage extensions: exercise the optional/conditional paths not hit above.

        test("ActionRow renders without id") {
            runComposeUiTest {
                setContent {
                    AgentDeskTheme(mode = ThemeMode.Light) {
                        ActionRow(title = "No-id task", tone = StatusTone.Blocked, statusLabel = "Blocked")
                    }
                }
                onNodeWithText("No-id task").assertIsDisplayed()
                onNodeWithText("Blocked").assertIsDisplayed()
            }
        }

        test("EventRow renders without divider") {
            runComposeUiTest {
                setContent {
                    AgentDeskTheme(mode = ThemeMode.Light) {
                        EventRow(
                            type = "Build started",
                            occurredAt = "09:00",
                            detail = "Running CI",
                            source = "AD-200",
                            showDivider = false,
                        )
                    }
                }
                onNodeWithText("Build started").assertIsDisplayed()
            }
        }

        test("Panel renders without count and with explicit titleColor") {
            runComposeUiTest {
                setContent {
                    AgentDeskTheme(mode = ThemeMode.Light) {
                        Panel(title = "Events", titleColor = Color.Red) {
                            EvidenceItem(label = "evidence.log")
                        }
                    }
                }
                onNodeWithText("Events").assertIsDisplayed()
            }
        }

        test("ThemeModeControl renders in System mode") {
            runComposeUiTest {
                setContent {
                    AgentDeskTheme(mode = ThemeMode.Light) {
                        ThemeModeControl(mode = ThemeMode.System, onCycle = {})
                    }
                }
                onNodeWithText("Theme: Auto").assertIsDisplayed()
            }
        }

        test("AgentDeskTheme status colors accessible in composition") {
            runComposeUiTest {
                setContent {
                    AgentDeskTheme(mode = ThemeMode.Light) {
                        // Touch the status accessor, then render a labelled pill we can assert on.
                        AgentDeskTheme.status
                        StatusPill(label = "Neutral", tone = StatusTone.Neutral)
                    }
                }
                onNodeWithText("Neutral").assertIsDisplayed()
            }
        }

        test("UI text defaults to the Inter family inside the theme") {
            var defaultFamily: FontFamily? = null
            var uiFamily: FontFamily? = null
            runComposeUiTest {
                setContent {
                    AgentDeskTheme(mode = ThemeMode.Light) {
                        defaultFamily = LocalTextStyle.current.fontFamily
                        uiFamily = AgentDeskTheme.typography.uiFamily
                    }
                }
            }
            defaultFamily shouldBe uiFamily
        }
    })
