package com.yonatankarp.agentdesk.core.domain.valueobjects

object IdentifierGrammar {
    private val validPattern = "[a-z][a-z0-9]*(?:[._:-][a-z0-9]+){0,7}".toRegex()

    fun normalize(
        raw: String,
        fieldName: String,
        errorMessage: String,
    ): String {
        val normalized = raw.trim().lowercase()
        require(validPattern.matches(normalized)) {
            errorMessage
        }
        PublicSafeTextPolicy.requirePublicSafe(normalized, fieldName = fieldName)
        return normalized
    }
}
