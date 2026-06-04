package com.yonatankarp.agentdesk.core.domain.valueobjects

internal class SingleLineWorkText private constructor(val value: String) {
    companion object {
        fun parse(
            raw: String,
            fieldName: String,
            maxLength: Int,
        ): SingleLineWorkText {
            val normalized = PublicSafeTextPolicy.normalize(raw, fieldName, maxLength)
            PublicSafeTextPolicy.requirePublicSafe(normalized, fieldName)
            PublicSafeTextPolicy.requirePublicUrlIfUrl(normalized, fieldName)
            return SingleLineWorkText(normalized)
        }
    }
}
