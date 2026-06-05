package com.yonatankarp.agentdesk.core.domain.valueobjects

object PublicSafeTextPolicy {
    private val whitespace = """\s+""".toRegex()
    private val windowsPath = """^[A-Za-z]:[\\/].*""".toRegex()
    private val rawChannelOrMessageId = """\b\d{17,20}\b""".toRegex()
    private val secretMarkers =
        listOf(
            "auth_token",
            "bearer ",
            "github_pat_",
            "ghp_",
            "op://",
            "password",
            "secret",
            "token",
            "xoxb-",
        )
    private val privateMarkers =
        listOf(
            "/home/",
            "/users/",
            "\\users\\",
            "[subagent",
            "<conversation",
            "agent:main:",
            "channel:",
            "discord channel",
            "message:",
            listOf("open", "claw runtime context").joinToString(""),
            listOf("raw", "transcript").joinToString(" "),
            "session:",
            "thread:",
        )

    fun normalize(
        raw: String,
        fieldName: String,
        maxLength: Int,
    ): String {
        val normalized = raw.trim().replace(whitespace, " ")
        require(normalized.isNotEmpty()) {
            "$fieldName must not be blank"
        }
        require(raw.lines().size == 1) {
            "$fieldName must be a single line"
        }
        require(normalized.length <= maxLength) {
            "$fieldName must be $maxLength characters or fewer"
        }
        return normalized
    }

    fun requirePublicSafe(
        normalized: String,
        fieldName: String,
    ) {
        requireNoPrivateLocalPaths(normalized, fieldName)
        requireNoPrivateRuntimeOrSecrets(normalized, fieldName)
    }

    fun normalizeAndRequirePublicSafe(
        raw: String,
        fieldName: String,
        maxLength: Int,
    ): String {
        val normalized = normalize(raw, fieldName = fieldName, maxLength = maxLength)
        requirePublicSafe(normalized, fieldName = fieldName)
        return normalized
    }

    fun normalizeAndRequirePublicSafeLocalConfigPath(
        raw: String,
        fieldName: String,
        maxLength: Int,
    ): String {
        val normalized = normalize(raw, fieldName = fieldName, maxLength = maxLength)
        requireNoPrivateRuntimeOrSecrets(normalized, fieldName = fieldName)
        return normalized
    }

    fun normalizeAndRequirePublicSafeText(
        raw: String,
        fieldName: String,
        maxLength: Int,
    ): String {
        val normalized = normalizeAndRequirePublicSafe(raw, fieldName = fieldName, maxLength = maxLength)
        requirePublicUrlIfUrl(normalized, fieldName = fieldName)
        return normalized
    }

    fun requirePublicUrlIfUrl(
        normalized: String,
        fieldName: String,
    ) {
        val lower = normalized.lowercase()
        if ("://" !in lower) {
            return
        }

        val parsedUrl = ParsedPublicUrl.parse(normalized, fieldName)

        require(parsedUrl.scheme == "https") {
            "$fieldName URLs must use https"
        }
        require(!parsedUrl.hasUserInfo) {
            "$fieldName URLs must not include userinfo"
        }
        require(!parsedUrl.host.isPrivateOrLocalHost(parsedUrl.bracketedHost)) {
            "$fieldName URLs must not point to private hosts"
        }
    }

    private fun requireNoPrivateLocalPaths(
        normalized: String,
        fieldName: String,
    ) {
        val lower = normalized.lowercase()
        require(!normalized.startsWith("/") && !normalized.startsWith("~/") && !windowsPath.matches(normalized)) {
            "$fieldName must not include private local paths"
        }
        require("\\" !in normalized && !lower.startsWith("file:")) {
            "$fieldName must not include private local paths"
        }
    }

    private fun requireNoPrivateRuntimeOrSecrets(
        normalized: String,
        fieldName: String,
    ) {
        val lower = normalized.lowercase()
        require(secretMarkers.none { marker -> marker in lower }) {
            "$fieldName must not include secrets or credential markers"
        }
        require(privateMarkers.none { marker -> marker in lower }) {
            "$fieldName must not include private runtime, channel, or transcript markers"
        }
        require(!rawChannelOrMessageId.containsMatchIn(normalized)) {
            "$fieldName must not include raw channel or message identifiers"
        }
    }

    private data class ParsedPublicUrl(
        val scheme: String,
        val host: String,
        val bracketedHost: Boolean,
        val hasUserInfo: Boolean,
    ) {
        companion object {
            fun parse(
                raw: String,
                fieldName: String,
            ): ParsedPublicUrl {
                val schemeEnd = raw.indexOf("://")
                require(schemeEnd > 0) {
                    "$fieldName URLs must include a scheme"
                }

                val scheme = raw.substring(0, schemeEnd).lowercase()
                val remainder = raw.substring(schemeEnd + 3)
                val authorityEnd =
                    listOfNotNull(
                        remainder.indexOf('/').takeUnless { it < 0 },
                        remainder.indexOf('?').takeUnless { it < 0 },
                        remainder.indexOf('#').takeUnless { it < 0 },
                    ).minOrNull() ?: remainder.length
                val authority = remainder.substring(0, authorityEnd)
                require(authority.isNotBlank()) {
                    "$fieldName URLs must include a host"
                }

                val hasUserInfo = "@" in authority
                val hostAuthority = if (hasUserInfo) authority.substringAfterLast("@") else authority
                val host = hostAuthority.hostWithoutPort(fieldName)

                return ParsedPublicUrl(
                    scheme = scheme,
                    host = host,
                    bracketedHost = hostAuthority.startsWith("["),
                    hasUserInfo = hasUserInfo,
                )
            }
        }
    }

    private fun String.hostWithoutPort(fieldName: String): String {
        val host =
            if (startsWith("[")) {
                val closingBracket = indexOf(']')
                require(closingBracket > 1) {
                    "$fieldName URLs must include a host"
                }
                val afterBracket = substring(closingBracket + 1)
                require(afterBracket.isEmpty() || afterBracket.matches(""":[0-9]+""".toRegex())) {
                    "$fieldName URLs must include a valid host"
                }
                substring(1, closingBracket)
            } else {
                require(count { it == ':' } <= 1) {
                    "$fieldName URLs must include a valid host"
                }
                val colon = indexOf(':')
                if (colon >= 0) {
                    require(colon > 0 && substring(colon + 1).matches("""[0-9]+""".toRegex())) {
                        "$fieldName URLs must include a valid host"
                    }
                    substring(0, colon)
                } else {
                    this
                }
            }.trim().trimEnd('.').lowercase()

        require(host.isNotBlank()) {
            "$fieldName URLs must include a host"
        }
        return host
    }

    private fun String.isPrivateOrLocalHost(bracketedHost: Boolean): Boolean = isLocalHostname() ||
        isUnsafeIpv4Host() ||
        isAmbiguousNumericHost() ||
        isUnsafeIpv6Host(bracketedHost)

    private fun String.isLocalHostname(): Boolean = this == "localhost" ||
        endsWith(".localhost") ||
        endsWith(".local") ||
        endsWith(".localdomain")

    private fun String.isUnsafeIpv4Host(): Boolean {
        val octets =
            split(".")
                .takeIf { it.size in 2..4 && it.all { part -> part.isNotEmpty() && part.all(Char::isDigit) } }
                ?.map { it.toIntOrNull() }
                ?: return false
        if (octets.any { it == null || it !in 0..255 }) {
            return true
        }
        val first = octets[0] ?: return true
        val second = octets[1] ?: return true

        return first == 0 ||
            first == 10 ||
            first == 127 ||
            (first == 169 && second == 254) ||
            (first == 172 && second in 16..31) ||
            (first == 192 && second == 168) ||
            first in 224..255
    }

    private fun String.isAmbiguousNumericHost(): Boolean = all(Char::isDigit) ||
        startsWith("0x") ||
        split(".").any { part -> part.length > 1 && part.startsWith("0") && part.all(Char::isDigit) }

    private fun String.isUnsafeIpv6Host(bracketedHost: Boolean): Boolean {
        if (!bracketedHost && ":" in this) {
            return true
        }
        if (":" !in this) {
            return false
        }

        val firstGroup = substringBefore(":")
        val mappedIpv4 = substringAfterLast(":").takeIf { "." in it }
        return this == "::" ||
            this == "::1" ||
            isCompressedLoopbackOrUnspecifiedIpv6() ||
            isExpandedLoopbackOrUnspecifiedIpv6() ||
            mappedIpv4?.isUnsafeIpv4Host() == true ||
            firstGroup in "fe80".."febf" ||
            startsWith("fc") ||
            startsWith("fd") ||
            startsWith("ff")
    }

    private fun String.isCompressedLoopbackOrUnspecifiedIpv6(): Boolean {
        if (!startsWith("::") || drop(2).contains(":")) {
            return false
        }
        val tail = drop(2)
        if (tail.isEmpty() || tail.length > 4 || !tail.all { it.isDigit() || it in 'a'..'f' }) {
            return false
        }
        return tail.trimStart('0').ifEmpty { "0" } in setOf("0", "1")
    }

    private fun String.isExpandedLoopbackOrUnspecifiedIpv6(): Boolean {
        val groups = split(":")
        if (groups.size != 8 || groups.any { group -> group.isEmpty() || group.length > 4 }) {
            return false
        }

        val normalizedGroups =
            groups.map { group ->
                group.trimStart('0').ifEmpty { "0" }
            }
        return normalizedGroups.take(7).all { it == "0" } && normalizedGroups.last() in setOf("0", "1")
    }
}
