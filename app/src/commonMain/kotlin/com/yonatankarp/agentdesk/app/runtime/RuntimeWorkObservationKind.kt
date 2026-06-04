package com.yonatankarp.agentdesk.app.runtime

enum class RuntimeWorkObservationKind {
    Started,
    NeedsDecision,
    Blocked,
    Succeeded,
    Failed,
    Canceled,
}
