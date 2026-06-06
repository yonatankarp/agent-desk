package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.SampleOperatorState
import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary
import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class DesktopSmokeSnapshotTest :
    BehaviorSpec({
        given("the desktop smoke snapshot builder") {
            `when`("building from the sample operator state") {
                then("it exposes the expected desktop sections") {
                    val snapshot = DesktopSmokeSnapshotBuilder.from(SampleOperatorState.current())
                    val text = snapshot.flattenedText()

                    text shouldContain "Agent Desk"
                    text shouldContain "Sample state"
                    text shouldContain "Replay status"
                    text shouldContain "Work state"
                    text shouldContain "Read-only timeline"
                    text shouldContain "Decision queue"
                    text shouldContain "Evidence drilldown"
                    text shouldContain "Not done: 2 item(s) need operator attention."
                    text shouldContain "Discovery/no-issue output is triage, not product completion."
                    text shouldContain "[Running] agent-task:42 Run public hygiene check"
                    text shouldContain "work.started agent-task:42 from sample-agent"
                    text shouldContain "[Needs decision] agent-task:43 Choose adapter boundary"
                    text shouldContain "[Blocked] agent-task:44 Review build failure"
                    text shouldContain "Observation: work.blocked for agent-task:44"
                    text shouldContain "Source: sample-agent"
                    text shouldContain "Provenance: replay event event:agent-task:44:blocked"
                    text shouldContain "Criteria result: Not done: 2 item(s) need operator attention."
                    text shouldContain "Evidence missing: no public-safe evidence reference was attached."
                    text shouldContain "Operator notes: unavailable in read-only proof."
                    text shouldContain "Redacted evidence: raw provider payloads are not rendered."
                    text shouldContain
                        "Diagnostics: raw provider data and arbitrary local file opening are unavailable by design."
                }
            }

            `when`("building from an empty operator state") {
                then("it exposes explicit empty rows") {
                    val snapshot =
                        DesktopSmokeSnapshotBuilder.from(
                            DesktopScreenState.Ready(
                                state = OperatorState(workItems = emptyList(), events = emptyList()),
                                modeLabel = "Loaded state",
                            ),
                        )

                    snapshot.modeLabel shouldBe "Loaded state"
                    snapshot.summary shouldBe "0 active / 0 attention"
                    snapshot.sectionRows("Replay status").joinToString("\n") shouldContain
                        "Empty queue: no current work or decisions; not product completion without milestone evidence."
                    snapshot.sectionRows("Work state") shouldBe listOf("No current work")
                    snapshot.sectionRows("Read-only timeline") shouldBe listOf("No recent events")
                    snapshot.sectionRows("Decision queue") shouldBe listOf("No items need a decision")
                    snapshot.sectionRows("Evidence drilldown") shouldBe listOf(
                        "Evidence missing: no replay events are available.",
                        "Criteria result: Empty queue: no current work or decisions; not product completion without milestone evidence.",
                        "Operator notes: unavailable in read-only proof.",
                        "Diagnostics: raw provider data and arbitrary local file opening are unavailable by design.",
                    )
                }
            }

            `when`("an item needs operator attention") {
                then("it appears in the attention queue") {
                    val item =
                        WorkItem(
                            id = WorkItemId.parse("agent-task:91"),
                            title = WorkItemTitle.parse("Choose retry path"),
                            status = WorkStatus.NeedsDecision,
                            summary = WorkSummary.parse("Operator must choose whether to retry."),
                        )

                    val snapshot =
                        DesktopSmokeSnapshotBuilder.from(
                            DesktopScreenState.Ready(
                                state = OperatorState(workItems = listOf(item), events = emptyList()),
                                modeLabel = "Loaded state",
                            ),
                        )

                    snapshot.summary shouldBe "1 active / 1 attention"
                    snapshot.sectionRows("Decision queue") shouldBe listOf(
                        "[Needs decision] agent-task:91 Choose retry path - Operator must choose whether to retry.",
                    )
                }
            }

            `when`("inspecting the flattened sample text") {
                then("the snapshot remains public safe") {
                    val text = DesktopSmokeSnapshotBuilder.from(SampleOperatorState.current()).flattenedText()

                    text.shouldBePublicSafe()
                }
            }

            `when`("building from the loading state") {
                then("it explains unavailable evidence") {
                    val snapshot = DesktopSmokeSnapshotBuilder.from(DesktopScreenState.Loading)

                    snapshot.sectionRows("Evidence drilldown") shouldBe listOf(
                        "Evidence unavailable until operator state is loaded.",
                        "Diagnostics: raw provider data and arbitrary local file opening are unavailable by design.",
                    )
                }
            }
        }
    })

private fun DesktopSmokeSnapshot.sectionRows(title: String): List<String> = sections.single { it.title == title }.rows
