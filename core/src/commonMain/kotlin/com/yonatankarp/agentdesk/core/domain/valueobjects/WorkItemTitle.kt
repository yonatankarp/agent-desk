package com.yonatankarp.agentdesk.core.domain.valueobjects

@JvmInline
value class WorkItemTitle private constructor(val value: String) {
    companion object {
        private const val MAX_LENGTH = 120
        private const val FIELD_NAME = "Work item title"

        fun parse(raw: String): WorkItemTitle = WorkItemTitle(
            SingleLineWorkText.parse(
                raw = raw,
                fieldName = FIELD_NAME,
                maxLength = MAX_LENGTH,
            ).value,
        )
    }

    override fun toString(): String = value
}
