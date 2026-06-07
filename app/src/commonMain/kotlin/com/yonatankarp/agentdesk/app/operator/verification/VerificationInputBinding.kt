package com.yonatankarp.agentdesk.app.operator.verification

import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp

/**
 * Binds a verification result to the immutable content it checked. The digest
 * detects drift — whether the artifact changed after it was verified — not
 * forgery: results are constructed in-process and nothing signs them.
 */
data class VerificationInputBinding(
    val digest: ContentDigest,
    val algorithm: DigestAlgorithm,
    val capturedAt: EventTimestamp,
)

enum class DigestAlgorithm {
    Sha256,
}

/**
 * A content digest over a public-safe artifact. The fixed lowercase-hex charset
 * is the public-safety guarantee: it cannot carry a path, secret, or private id.
 */
@JvmInline
value class ContentDigest private constructor(val value: String) {
    companion object {
        private val sha256Hex = "[0-9a-f]{64}".toRegex()

        fun parseSha256(raw: String): ContentDigest {
            require(sha256Hex.matches(raw)) {
                "Content digest must be a 64-character lowercase SHA-256 hex string"
            }
            return ContentDigest(raw)
        }
    }

    override fun toString(): String = value
}
