package com.yonatankarp.agentdesk.app.runtime

import com.yonatankarp.agentdesk.core.domain.valueobjects.PublicSafeTextPolicy

@JvmInline
value class RuntimeHostCredentialReference private constructor(val value: String) {
    companion object {
        fun parse(raw: String): RuntimeHostCredentialReference {
            val normalized = try {
                PublicSafeTextPolicy.normalizeAndRequirePublicSafeLocalConfigPath(
                    raw = raw,
                    fieldName = "hostCredentialReference",
                    maxLength = 512,
                )
            } catch (error: IllegalArgumentException) {
                throw RuntimeHostReachabilityException(
                    message = error.message ?: "hostCredentialReference must be a single local config value",
                    cause = error,
                )
            }

            return RuntimeHostCredentialReference(normalized)
        }
    }

    override fun toString(): String = "<local-host-credential-reference>"
}
