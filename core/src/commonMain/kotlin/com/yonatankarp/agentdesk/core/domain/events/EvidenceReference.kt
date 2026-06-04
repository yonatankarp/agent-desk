package com.yonatankarp.agentdesk.core.domain.events

data class EvidenceReference(
    val kind: EvidenceReferenceKind,
    val label: EvidenceLabel,
    val target: EvidenceTarget,
)

enum class EvidenceReferenceKind(val wireName: String) {
    Commit("commit"),
    CheckRun("check-run"),
    Artifact("artifact"),
    Screenshot("screenshot"),
    SanitizedNote("sanitized-note"),
    ;

    companion object {
        fun fromWireName(raw: String): EvidenceReferenceKind = entries.firstOrNull { it.wireName == raw }
            ?: throw IllegalArgumentException("Unknown evidence reference kind: $raw")
    }
}

@JvmInline
value class EvidenceLabel private constructor(val value: String) {
    companion object {
        fun parse(raw: String): EvidenceLabel {
            val normalized = PublicSafeEvidenceText.normalize(raw, fieldName = "Evidence label", maxLength = 80)
            PublicSafeEvidenceText.requirePublicSafe(normalized, fieldName = "Evidence label")
            return EvidenceLabel(normalized)
        }
    }

    override fun toString(): String = value
}

@JvmInline
value class EvidenceTarget private constructor(val value: String) {
    companion object {
        fun parse(raw: String): EvidenceTarget {
            val normalized = PublicSafeEvidenceText.normalize(raw, fieldName = "Evidence target", maxLength = 256)
            PublicSafeEvidenceText.requirePublicSafe(normalized, fieldName = "Evidence target")
            PublicSafeEvidenceText.requirePublicUrlIfUrl(normalized)
            return EvidenceTarget(normalized)
        }
    }

    override fun toString(): String = value
}

private object PublicSafeEvidenceText {
    private val whitespace = """\s+""".toRegex()
    private val windowsPath = """^[A-Za-z]:[\\/].*""".toRegex()
    private val discordSnowflake = """\b\d{17,20}\b""".toRegex()
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
            "openclaw runtime context",
            "raw transcript",
            "session:",
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
        val lower = normalized.lowercase()
        require(!normalized.startsWith("/") && !normalized.startsWith("~/") && !windowsPath.matches(normalized)) {
            "$fieldName must not include private local paths"
        }
        require("\\" !in normalized && !lower.startsWith("file:")) {
            "$fieldName must not include private local paths"
        }
        require(secretMarkers.none { marker -> marker in lower }) {
            "$fieldName must not include secrets or credential markers"
        }
        require(privateMarkers.none { marker -> marker in lower }) {
            "$fieldName must not include private runtime, channel, or transcript markers"
        }
        require(!discordSnowflake.containsMatchIn(normalized)) {
            "$fieldName must not include raw channel or message identifiers"
        }
    }

    fun requirePublicUrlIfUrl(normalized: String) {
        val lower = normalized.lowercase()
        if ("://" !in lower) {
            return
        }

        require(lower.startsWith("https://")) {
            "Evidence target URLs must use https"
        }
        require(
            listOf(
                "https://localhost",
                "https://127.",
                "https://10.",
                "https://172.16.",
                "https://172.17.",
                "https://172.18.",
                "https://172.19.",
                "https://172.20.",
                "https://172.21.",
                "https://172.22.",
                "https://172.23.",
                "https://172.24.",
                "https://172.25.",
                "https://172.26.",
                "https://172.27.",
                "https://172.28.",
                "https://172.29.",
                "https://172.30.",
                "https://172.31.",
                "https://192.168.",
            ).none { prefix -> lower.startsWith(prefix) } && !lower.contains(".local"),
        ) {
            "Evidence target URLs must not point to private hosts"
        }
    }
}
