package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.core.domain.events.EvidenceReference

object EvidenceDisplayFormatter {
    fun format(reference: EvidenceLine): String = format(
        kind = reference.kind,
        label = reference.label,
        target = reference.target,
    )

    fun format(reference: EvidenceReference): String = format(
        kind = reference.kind.wireName,
        label = reference.label.toString(),
        target = reference.target.toString(),
    )

    fun format(
        kind: String,
        label: String,
        target: String,
    ): String = "$kind $label -> $target"
}
