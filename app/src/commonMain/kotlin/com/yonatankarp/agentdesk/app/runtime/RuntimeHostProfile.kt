package com.yonatankarp.agentdesk.app.runtime

data class RuntimeHostProfile(
    val alias: RuntimeHostAlias,
    val endpoint: RuntimeHostEndpoint,
    val aliasMappings: List<RuntimeHostAliasMapping> = emptyList(),
    val authState: RuntimeHostAuthState = RuntimeHostAuthState.NotConfigured,
    val permissionMode: RuntimeHostPermissionMode = RuntimeHostPermissionMode.Unsupported,
    val observationBridge: RuntimeHostObservationBridge? = null,
) {
    fun publicSummary(): String = "Runtime host config host=${alias.value} endpoint=local-only."

    fun accessBoundary(): RuntimeHostAccessBoundary = RuntimeHostAccessBoundary(
        alias = alias.takeUnless { authState == RuntimeHostAuthState.NotConfigured },
        authState = authState,
        permissionMode = permissionMode,
    )

    override fun toString(): String = "RuntimeHostProfile(alias=${alias.value}, endpoint=local-only, aliasMappings=${aliasMappings.size}, " +
        "auth=${authState.wireName}, mode=${permissionMode.wireName}, observationBridge=local-only)"
}
