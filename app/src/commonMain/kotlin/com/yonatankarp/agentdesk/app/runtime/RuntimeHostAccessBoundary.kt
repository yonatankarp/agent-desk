package com.yonatankarp.agentdesk.app.runtime

data class RuntimeHostAccessBoundary(
    val alias: RuntimeHostAlias? = null,
    val authState: RuntimeHostAuthState,
    val permissionMode: RuntimeHostPermissionMode,
) {
    init {
        if (authState == RuntimeHostAuthState.NotConfigured && alias != null) {
            throw RuntimeHostReachabilityException("not-configured host auth must not include an alias")
        }
        if (authState == RuntimeHostAuthState.Accepted && alias == null) {
            throw RuntimeHostReachabilityException("accepted host auth requires an alias")
        }
        if (authState != RuntimeHostAuthState.Accepted && permissionMode == RuntimeHostPermissionMode.ActionCapable) {
            throw RuntimeHostReachabilityException("action-capable mode requires accepted host auth")
        }
    }

    fun allows(operation: RuntimeHostOperation): Boolean = authState == RuntimeHostAuthState.Accepted && permissionMode.allows(operation)

    fun publicMessage(): String {
        val host = alias?.value ?: "not-configured"
        val allowed = permissionMode.allowedOperationNames().joinToString(",").ifEmpty { "none" }
        return "Host access: host=$host auth=${authState.wireName} mode=${permissionMode.wireName} allowed=$allowed."
    }

    override fun toString(): String = publicMessage()
}
