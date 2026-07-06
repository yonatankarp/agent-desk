package com.yonatankarp.agentdesk.app.operator.action

import com.yonatankarp.agentdesk.core.domain.valueobjects.PublicSafeTextPolicy

sealed interface LiveHostInspectAdapterResult {
    data class Succeeded(val output: LiveHostInspectOutput) : LiveHostInspectAdapterResult

    data class Failed(val reason: String) : LiveHostInspectAdapterResult {
        init {
            require(reason.isNotBlank()) { "Inspect failure reason must not be blank." }
            PublicSafeTextPolicy.requirePublicSafe(reason, fieldName = "Inspect failure reason")
        }
    }

    data class UnsafeOutput(val reason: String) : LiveHostInspectAdapterResult {
        init {
            require(reason.isNotBlank()) { "Unsafe inspect reason must not be blank." }
            PublicSafeTextPolicy.requirePublicSafe(reason, fieldName = "Unsafe inspect reason")
        }
    }
}
