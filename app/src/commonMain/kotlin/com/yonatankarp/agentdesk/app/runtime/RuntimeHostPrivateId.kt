package com.yonatankarp.agentdesk.app.runtime

import com.yonatankarp.agentdesk.core.domain.valueobjects.PublicSafeTextPolicy

@JvmInline
value class RuntimeHostPrivateId private constructor(val value: String) {
    companion object {
        fun parse(raw: String): RuntimeHostPrivateId {
            val normalized = try {
                PublicSafeTextPolicy.normalizeAndRequirePublicSafeLocalConfigPath(
                    raw = raw,
                    fieldName = "runtimeHostId",
                    maxLength = 120,
                )
            } catch (error: IllegalArgumentException) {
                throw RuntimeHostReachabilityException(
                    message = error.message ?: "runtimeHostId must be a single local config value",
                    cause = error,
                )
            }

            if ("://" in normalized || "=" in normalized) {
                throw RuntimeHostReachabilityException("runtimeHostId must not include endpoint or assignment details")
            }

            return RuntimeHostPrivateId(normalized)
        }
    }

    override fun toString(): String = "<local-runtime-host-id>"
}
