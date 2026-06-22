package com.yonatankarp.agentdesk.app.runtime

data class RuntimeHostProfile(
    val alias: RuntimeHostAlias,
    val endpoint: RuntimeHostEndpoint,
    val aliasMappings: List<RuntimeHostAliasMapping> = emptyList(),
) {
    fun publicSummary(): String = "Runtime host config host=${alias.value} endpoint=local-only."

    override fun toString(): String = "RuntimeHostProfile(alias=${alias.value}, endpoint=local-only, aliasMappings=${aliasMappings.size})"
}
