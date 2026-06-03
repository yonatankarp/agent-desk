package com.yonatankarp.agentdesk.core.domain.valueobjects

internal class SingleLineWorkText private constructor(val value: String) {
    companion object {
        private val whitespace = """\s+""".toRegex()

        fun parse(
            raw: String,
            fieldName: String,
            maxLength: Int,
        ): SingleLineWorkText {
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
            return SingleLineWorkText(normalized)
        }
    }
}
