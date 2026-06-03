package com.yonatankarp.agentdesk.core.domain.valueobjects

@JvmInline
value class WorkSummary private constructor(val value: String) {
    companion object {
        private const val MAX_LENGTH = 280
        private const val FIELD_NAME = "Work summary"

        fun parse(raw: String): WorkSummary = WorkSummary(
            SingleLineWorkText.parse(
                raw = raw,
                fieldName = FIELD_NAME,
                maxLength = MAX_LENGTH,
            ).value,
        )
    }

    override fun toString(): String = value
}
