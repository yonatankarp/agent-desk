package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.core.domain.valueobjects.PublicSafeTextPolicy

@JvmInline
value class Actor private constructor(val value: String) {
    companion object {
        private val validPattern = "[a-z][a-z0-9]*(?:[._:-][a-z0-9]+){0,7}".toRegex()

        fun parse(raw: String): Actor {
            val normalized = raw.trim().lowercase()
            require(validPattern.matches(normalized)) {
                "Actor must be a lowercase identifier such as operator:daily-agent"
            }
            PublicSafeTextPolicy.requirePublicSafe(normalized, fieldName = "Actor")
            return Actor(normalized)
        }
    }

    override fun toString(): String = value
}
