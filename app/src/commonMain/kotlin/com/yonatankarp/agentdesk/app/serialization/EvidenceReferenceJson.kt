package com.yonatankarp.agentdesk.app.serialization

import com.yonatankarp.agentdesk.core.domain.events.EvidenceLabel
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReference
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReferenceKind
import com.yonatankarp.agentdesk.core.domain.events.EvidenceTarget

internal fun EvidenceReference.toRecord(): EvidenceReferenceRecord = EvidenceReferenceRecord(
    kind = kind.wireName,
    label = label.toString(),
    target = target.toString(),
)

internal fun EvidenceReferenceRecord.toDomain(): EvidenceReference = EvidenceReference(
    kind = EvidenceReferenceKind.fromWireName(kind),
    label = EvidenceLabel.parse(label),
    target = EvidenceTarget.parse(target),
)
