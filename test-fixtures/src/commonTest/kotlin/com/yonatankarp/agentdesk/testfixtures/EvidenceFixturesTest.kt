package com.yonatankarp.agentdesk.testfixtures

import com.yonatankarp.agentdesk.core.domain.events.EvidenceReferenceKind
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class EvidenceFixturesTest :
    BehaviorSpec({
        given("evidence factories") {
            `when`("a check-run reference is built from raw strings") {
                then("kind, label, and target are parsed") {
                    val reference =
                        checkRunEvidence(
                            label = "Gradle Build",
                            target = "https://github.com/yonatankarp/agent-desk/actions/runs/1",
                        )

                    reference.kind shouldBe EvidenceReferenceKind.CheckRun
                    reference.label.value shouldBe "Gradle Build"
                    reference.target.value shouldBe "https://github.com/yonatankarp/agent-desk/actions/runs/1"
                }
            }

            `when`("the other kinds are built") {
                then("each maps to its kind") {
                    commitEvidence("Implementation commit", "commit:80de329").kind shouldBe
                        EvidenceReferenceKind.Commit
                    sanitizedNoteEvidence("Decision context", "docs/decision-context.md").kind shouldBe
                        EvidenceReferenceKind.SanitizedNote
                    artifactEvidence("Verification output", "artifact:verification-output").kind shouldBe
                        EvidenceReferenceKind.Artifact
                    screenshotEvidence("Desktop smoke", "artifact:desktop-smoke.png").kind shouldBe
                        EvidenceReferenceKind.Screenshot
                }
            }
        }
    })
