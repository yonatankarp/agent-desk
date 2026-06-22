package com.yonatankarp.agentdesk.app.runtime

object RuntimeHostProfileConfigParser {
    fun parse(values: Map<String, String>): RuntimeHostProfile {
        val alias = values.required("hostAlias")
            .let(RuntimeHostAlias::parse)
        val endpoint = values.required("hostEndpoint")
            .let(RuntimeHostEndpoint::parse)
        val aliasMappings = values["hostAliasMappings"]
            ?.let(::parseAliasMappings)
            .orEmpty()

        return RuntimeHostProfile(
            alias = alias,
            endpoint = endpoint,
            aliasMappings = aliasMappings,
        )
    }

    private fun Map<String, String>.required(key: String): String = this[key]
        ?.takeIf { it.isNotBlank() }
        ?: throw RuntimeHostReachabilityException("$key must be configured")

    private fun parseAliasMappings(raw: String): List<RuntimeHostAliasMapping> {
        if (raw.isBlank()) {
            return emptyList()
        }

        return raw.split(",").map { entry ->
            val parts = entry.split("=", limit = 2)
            if (parts.size != 2) {
                throw RuntimeHostReachabilityException("hostAliasMappings entries must use runtimeHostId=alias")
            }

            RuntimeHostAliasMapping(
                runtimeHostId = RuntimeHostPrivateId.parse(parts[0]),
                alias = RuntimeHostAlias.parse(parts[1]),
            )
        }
    }
}
