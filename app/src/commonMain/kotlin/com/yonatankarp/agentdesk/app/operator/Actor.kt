package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.core.domain.events.EventSource
import com.yonatankarp.agentdesk.core.domain.valueobjects.IdentifierGrammar

@JvmInline
value class Actor private constructor(val value: String) {
    companion object {
        fun parse(raw: String): Actor = Actor(
            IdentifierGrammar.normalize(
                raw = raw,
                fieldName = "Actor",
                errorMessage = "Actor must be a lowercase identifier such as operator:daily-agent",
            ),
        )

        fun from(source: EventSource): Actor = Actor(source.value)
    }

    override fun toString(): String = value
}
