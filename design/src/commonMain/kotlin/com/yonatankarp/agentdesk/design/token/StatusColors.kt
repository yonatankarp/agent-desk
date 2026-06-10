package com.yonatankarp.agentdesk.design.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.yonatankarp.agentdesk.app.operator.StatusTone

@Immutable
data class StatusRole(
    val text: Color,
    val rail: Color,
    val pillBg: Color,
)

@Immutable
data class StatusColors(
    val neutral: StatusRole,
    val active: StatusRole,
    val attention: StatusRole,
    val blocked: StatusRole,
    val success: StatusRole,
    val failure: StatusRole,
) {
    fun forTone(tone: StatusTone): StatusRole = when (tone) {
        StatusTone.Neutral -> neutral
        StatusTone.Active -> active
        StatusTone.Attention -> attention
        StatusTone.Blocked -> blocked
        StatusTone.Success -> success
        StatusTone.Failure -> failure
    }

    companion object {
        val Light = StatusColors(
            neutral = StatusRole(Color(0xFF4B5563), Color(0xFF94A3B8), Color(0xFFEEF1F5)),
            active = StatusRole(Color(0xFF0B6270), Color(0xFF0E7C8B), Color(0xFFE0F1F3)),
            // Attention rail is bright amber (#F08C00), clearly != Blocked red.
            // Attention text stays darker (#8A5200) for AA on its pill background.
            attention = StatusRole(Color(0xFF8A5200), Color(0xFFF08C00), Color(0xFFFBEEDC)),
            blocked = StatusRole(Color(0xFFB42121), Color(0xFFDC2626), Color(0xFFFBE5E5)),
            success = StatusRole(Color(0xFF136B33), Color(0xFF15803D), Color(0xFFE3F3E8)),
            failure = StatusRole(Color(0xFFA01818), Color(0xFFB91C1C), Color(0xFFFBE3E3)),
        )

        val Dark = StatusColors(
            neutral = StatusRole(Color(0xFFAEBED0), Color(0xFF64748B), Color(0xFF222F3D)),
            active = StatusRole(Color(0xFF5EE6D4), Color(0xFF2DD4BF), Color(0xFF0D332F)),
            attention = StatusRole(Color(0xFFFFD27A), Color(0xFFFBBF24), Color(0xFF33280A)),
            blocked = StatusRole(Color(0xFFFCA5A5), Color(0xFFF87171), Color(0xFF3A1818)),
            success = StatusRole(Color(0xFF86E0A2), Color(0xFF4ADE80), Color(0xFF103021)),
            failure = StatusRole(Color(0xFFFCA5A5), Color(0xFFEF4444), Color(0xFF3A1818)),
        )
    }
}
