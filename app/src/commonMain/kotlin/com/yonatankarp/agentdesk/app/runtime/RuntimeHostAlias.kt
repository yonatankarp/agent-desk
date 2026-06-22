package com.yonatankarp.agentdesk.app.runtime

import com.yonatankarp.agentdesk.core.domain.valueobjects.PublicSafeTextPolicy

@JvmInline
value class RuntimeHostAlias private constructor(val value: String) {
    companion object {
        fun parse(raw: String): RuntimeHostAlias {
            val normalized = try {
                PublicSafeTextPolicy.normalizeAndRequirePublicSafe(
                    raw = raw,
                    fieldName = "hostAlias",
                    maxLength = 80,
                )
            } catch (error: IllegalArgumentException) {
                throw RuntimeHostReachabilityException(
                    message = error.message ?: "hostAlias must be a public-safe alias",
                    cause = error,
                )
            }

            if ("://" in normalized || ":" in normalized) {
                throw RuntimeHostReachabilityException("hostAlias must not include endpoint details")
            }

            return RuntimeHostAlias(normalized)
        }
    }

    override fun toString(): String = value
}
