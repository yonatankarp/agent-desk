package com.yonatankarp.agentdesk.app.runtime

import com.yonatankarp.agentdesk.core.domain.valueobjects.PublicSafeTextPolicy

@JvmInline
value class RuntimeHostObservationBridge private constructor(val value: String) {
    companion object {
        fun parse(raw: String): RuntimeHostObservationBridge {
            val normalized = try {
                PublicSafeTextPolicy.normalizeAndRequirePublicSafeLocalConfigPath(
                    raw = raw,
                    fieldName = "hostObservationBridge",
                    maxLength = 512,
                )
            } catch (error: IllegalArgumentException) {
                throw RuntimeHostReachabilityException(
                    message = error.message ?: "hostObservationBridge must be a single local config value",
                    cause = error,
                )
            }

            return RuntimeHostObservationBridge(normalized)
        }
    }

    override fun toString(): String = "<local-host-observation-bridge>"
}
