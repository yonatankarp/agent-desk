package com.yonatankarp.agentdesk.mobile

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.yonatankarp.agentdesk.app.operator.mobile.MobileAttentionItem
import com.yonatankarp.agentdesk.app.operator.mobile.MobileEventLine
import com.yonatankarp.agentdesk.app.operator.mobile.MobileEvidenceReference
import com.yonatankarp.agentdesk.app.operator.mobile.MobileOperatorState
import com.yonatankarp.agentdesk.app.operator.mobile.MobileOperatorStateContract
import com.yonatankarp.agentdesk.app.operator.mobile.MobileProjectionWarning
import com.yonatankarp.agentdesk.app.operator.mobile.MobileStaleAttention
import com.yonatankarp.agentdesk.testfixtures.sanitizedNoteEvidence
import com.yonatankarp.agentdesk.testfixtures.workEvents
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain

// ExperimentalTestApi opt-in is tracked debt: see issue #279 — remove when
// Compose Multiplatform stabilizes the test API.
@OptIn(ExperimentalTestApi::class)
class MobileComposeSmokeTest :
    FunSpec({
        test("sample state renders visible shell text") {
            runComposeUiTest {
                setContent {
                    AgentDeskMobileApp(MobileOperatorStateContract.sample())
                }

                onNodeWithText("Agent Desk").assertIsDisplayed()
                onNodeWithText("3 current / 2 attention").assertIsDisplayed()
                onNodeWithText("Settings").assertIsDisplayed()
                onNodeWithText("Current work").assertIsDisplayed()
                onNodeWithText("Attention queue").assertIsDisplayed()
                onNodeWithText("Run public hygiene check").assertIsDisplayed()
                onAllNodesWithText("Choose adapter boundary").assertCountEquals(2)
                onAllNodesWithText("Review build failure").assertCountEquals(2)
                onNodeWithText("Recent events").performScrollTo().assertIsDisplayed()
            }
        }

        test("settings renders as a separate page") {
            runComposeUiTest {
                setContent {
                    AgentDeskMobileApp(MobileOperatorStateContract.sample())
                }

                onNodeWithText("Platform detail panels: host-specific capabilities stay platform-scoped").assertDoesNotExist()
                onNodeWithText("Settings").performClick()
                onNodeWithText("Platform detail panels: host-specific capabilities stay platform-scoped").assertIsDisplayed()
                onNodeWithText("Theme: Auto").assertIsDisplayed()
                onNodeWithText("Current work").assertDoesNotExist()
            }
        }

        test("empty state renders visible empty rows") {
            runComposeUiTest {
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
                onNodeWithText("No timeline entries").assertIsDisplayed()
                onAllNodesWithTag(MOBILE_EMPTY_ROW_TEST_TAG).assertCountEquals(4)
            }
        }

        test("timeline detail expands and collapses as a read-only disclosure") {
            runComposeUiTest {
                val state = MobileOperatorStateContract.fromEvents(
                    workEvents {
                        started()
                        blocked(evidence = listOf(sanitizedNoteEvidence("Blocked context", "docs/blocked-context.md")))
                    },
                )

                setContent {
                    AgentDeskMobileApp(state)
                }

                val provenance = "Provenance: replay event event:agent-task:42:blocked"
                onNodeWithText("Timeline").performScrollTo().assertIsDisplayed()
                onNodeWithText("Status: Read-only, Blocked").performScrollTo().assertIsDisplayed()
                onAllNodesWithText("agent-task:42 from mock-adapter").assertCountEquals(2)
                onNodeWithText(provenance).assertDoesNotExist()

                onAllNodesWithText("Details ▸")[1].performScrollTo().performClick()
                onNodeWithText("Details ▾").performScrollTo().assertIsDisplayed()
                onNodeWithText(provenance).performScrollTo().assertIsDisplayed()
                onNodeWithText("Source: mock-adapter").assertIsDisplayed()
                onNodeWithText("Decision: unavailable for latest replay event.").assertIsDisplayed()
                onNodeWithText("Criteria result: Not done: 1 item(s) need operator attention.").assertIsDisplayed()
                onNodeWithText("Related events: work.started").assertIsDisplayed()
                onNodeWithText("Redacted evidence: raw provider payloads are not rendered.").assertIsDisplayed()

                onNodeWithText("Details ▾").performClick()
                onNodeWithText(provenance).assertDoesNotExist()
                onNodeWithText("Successful outcome").assertDoesNotExist()
            }
        }

        test("terminal timeline rows render the projector outcome on both surfaces") {
            runComposeUiTest {
                val state = MobileOperatorStateContract.fromEvents(
                    workEvents {
                        started()
                        succeeded()
                    },
                )
                val terminalEntry = state.timeline.first { it.completionSummary != null }

                setContent {
                    AgentDeskMobileApp(state)
                }

                onNodeWithText("Timeline").performScrollTo().assertIsDisplayed()
                onNodeWithText(terminalEntry.completionSummary!!).performScrollTo().assertIsDisplayed()
                MobileDisplayText.timelineRow(terminalEntry) shouldContain terminalEntry.completionSummary!!
            }
        }

        test("dense timeline rows render long identifiers without clipping affordances") {
            runComposeUiTest {
                val longId = "a".repeat(60) + ":42"
                val state = MobileOperatorStateContract.fromEvents(
                    workEvents {
                        started(
                            workItemId = longId,
                            title = "Dense data wrap check",
                            summary = "A deliberately long summary line that must wrap onto multiple lines on a narrow viewport instead of clipping.",
                        )
                        blocked(
                            workItemId = longId,
                            evidence = listOf(
                                sanitizedNoteEvidence(
                                    "Dense evidence label",
                                    "docs/very/deep/path/to/a/sanitized-evidence-note-with-a-long-name.md",
                                ),
                            ),
                        )
                    },
                )

                setContent {
                    AgentDeskMobileApp(state)
                }

                onNodeWithText("Timeline").performScrollTo().assertIsDisplayed()
                onAllNodesWithText(longId).onFirst().assertIsDisplayed()
                onNodeWithText("Status: Read-only, Blocked").performScrollTo().assertIsDisplayed()
            }
        }

        test("stale evidence and warnings render visible text") {
            runComposeUiTest {
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

                onAllNodesWithText("check-run Mobile smoke -> https://github.com/yonatankarp/agent-desk/actions/runs/26937983933").assertCountEquals(3)
                onNodeWithText("Stale 1h 30m since 2026-06-02 21:00 UTC").performScrollTo().assertIsDisplayed()
                onNodeWithText("Evidence attached").performScrollTo().assertIsDisplayed()
                onNodeWithText("Projection warnings").performScrollTo().assertIsDisplayed()
                onNodeWithText("Cannot transition work item agent-task:91 from Succeeded to Blocked")
                    .performScrollTo()
                    .assertIsDisplayed()
            }
        }
    })
