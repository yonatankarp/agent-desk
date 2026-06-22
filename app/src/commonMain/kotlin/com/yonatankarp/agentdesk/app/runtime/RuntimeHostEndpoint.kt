package com.yonatankarp.agentdesk.app.runtime

import com.yonatankarp.agentdesk.core.domain.valueobjects.PublicSafeTextPolicy

@JvmInline
value class RuntimeHostEndpoint private constructor(val value: String) {
    companion object {
        fun parse(raw: String): RuntimeHostEndpoint {
            val normalized = try {
                PublicSafeTextPolicy.normalizeAndRequirePublicSafeLocalConfigPath(
                    raw = raw,
                    fieldName = "hostEndpoint",
                    maxLength = 512,
                )
            } catch (error: IllegalArgumentException) {
                throw RuntimeHostReachabilityException(
                    message = error.message ?: "hostEndpoint must be a single local config value",
                    cause = error,
                )
            }

            requireHttpEndpoint(normalized)
            requireNoUserInfo(normalized)
            return RuntimeHostEndpoint(normalized)
        }

        private fun requireHttpEndpoint(normalized: String) {
            val lower = normalized.lowercase()
            if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
                throw RuntimeHostReachabilityException("hostEndpoint must use http or https")
            }

            val authority = normalized.substringAfter("://").substringBefore("/")
            if (authority.isBlank()) {
                throw RuntimeHostReachabilityException("hostEndpoint must include a host")
            }
        }

        private fun requireNoUserInfo(normalized: String) {
            val authority = normalized.substringAfter("://").substringBefore("/")
            if ("@" in authority) {
                throw RuntimeHostReachabilityException("hostEndpoint must not include credentials")
            }
        }
    }

    override fun toString(): String = "<local-host-endpoint>"
}
