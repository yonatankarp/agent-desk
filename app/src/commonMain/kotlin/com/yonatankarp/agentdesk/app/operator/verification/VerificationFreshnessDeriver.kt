package com.yonatankarp.agentdesk.app.operator.verification

import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp

/**
 * Derives verification freshness from real timestamps instead of a
 * caller-supplied claim. Drift semantics: evidence is fresh only when its
 * input was captured at or after the work item's last change; otherwise it is
 * stale. Unbound or unverifiable evidence is conservatively unknown — it can
 * never count as fresh.
 */
object VerificationFreshnessDeriver {
    fun derive(
        binding: VerificationInputBinding?,
        lastChangedAt: EventTimestamp?,
    ): VerificationFreshness = when {
        binding == null || lastChangedAt == null -> VerificationFreshness.Unknown
        binding.capturedAt >= lastChangedAt -> VerificationFreshness.Fresh
        else -> VerificationFreshness.Stale
    }
}
