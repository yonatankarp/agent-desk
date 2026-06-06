package com.yonatankarp.agentdesk.testfixtures

import com.yonatankarp.agentdesk.core.domain.events.EvidenceLabel
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReference
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReferenceKind
import com.yonatankarp.agentdesk.core.domain.events.EvidenceTarget

fun commitEvidence(label: String, target: String): EvidenceReference = evidence(EvidenceReferenceKind.Commit, label, target)

fun checkRunEvidence(label: String, target: String): EvidenceReference = evidence(EvidenceReferenceKind.CheckRun, label, target)

fun sanitizedNoteEvidence(label: String, target: String): EvidenceReference = evidence(EvidenceReferenceKind.SanitizedNote, label, target)

private fun evidence(
    kind: EvidenceReferenceKind,
    label: String,
    target: String,
): EvidenceReference = EvidenceReference(
    kind = kind,
    label = EvidenceLabel.parse(label),
    target = EvidenceTarget.parse(target),
)
