package com.yonatankarp.agentdesk.core.domain.events

import com.yonatankarp.agentdesk.core.domain.valueobjects.PublicSafeTextPolicy

data class EvidenceReference(
    val kind: EvidenceReferenceKind,
    val label: EvidenceLabel,
    val target: EvidenceTarget,
)

enum class EvidenceReferenceKind(val wireName: String) {
    Commit("commit"),
    CheckRun("check-run"),
    Artifact("artifact"),
    Screenshot("screenshot"),
    SanitizedNote("sanitized-note"),
    ;

    companion object {
        fun fromWireName(raw: String): EvidenceReferenceKind = entries.firstOrNull { it.wireName == raw }
            ?: throw IllegalArgumentException("Unknown evidence reference kind: $raw")
    }
}

@JvmInline
value class EvidenceLabel private constructor(val value: String) {
    companion object {
        fun parse(raw: String): EvidenceLabel {
            val normalized = PublicSafeTextPolicy.normalize(raw, fieldName = "Evidence label", maxLength = 80)
            PublicSafeTextPolicy.requirePublicSafe(normalized, fieldName = "Evidence label")
            return EvidenceLabel(normalized)
        }
    }

    override fun toString(): String = value
}

@JvmInline
value class EvidenceTarget private constructor(val value: String) {
    companion object {
        fun parse(raw: String): EvidenceTarget {
            val normalized = PublicSafeTextPolicy.normalize(raw, fieldName = "Evidence target", maxLength = 256)
            PublicSafeTextPolicy.requirePublicSafe(normalized, fieldName = "Evidence target")
            PublicSafeTextPolicy.requirePublicUrlIfUrl(normalized, fieldName = "Evidence target")
            return EvidenceTarget(normalized)
        }
    }

    override fun toString(): String = value
}
