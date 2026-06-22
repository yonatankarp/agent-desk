package com.yonatankarp.agentdesk.app.runtime

data class RuntimeHostAliasMapping(
    val runtimeHostId: RuntimeHostPrivateId,
    val alias: RuntimeHostAlias,
) {
    override fun toString(): String = "RuntimeHostAliasMapping(runtimeHostId=local-only, alias=${alias.value})"
}
