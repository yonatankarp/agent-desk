package com.yonatankarp.agentdesk.design.token

import androidx.compose.ui.graphics.Color
import com.yonatankarp.agentdesk.app.operator.StatusTone
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import kotlin.math.pow

/**
 * Drift pin for WCAG AA. Muted text renders small (10-13sp) so needs >= 4.5:1
 * on every surface it sits on; status pill text sits on its own pillBg and must
 * also clear 4.5:1. Mirrors MobilePaletteContrastTest's luminance math.
 */
class AgentDeskContrastTest :
    FunSpec({
        test("muted and secondary text meet AA on every surface, both schemes") {
            listOf(AgentDeskColors.Light, AgentDeskColors.Dark).forEach { c ->
                listOf(c.background, c.panel, c.row).forEach { bg ->
                    contrastRatio(c.textMuted, bg) shouldBeGreaterThanOrEqual 4.5
                    contrastRatio(c.textSecondary, bg) shouldBeGreaterThanOrEqual 4.5
                }
            }
        }

        test("status pill text meets AA on its pill background, both schemes") {
            listOf(StatusColors.Light, StatusColors.Dark).forEach { sc ->
                StatusTone.entries.forEach { tone ->
                    val role = sc.forTone(tone)
                    contrastRatio(role.text, role.pillBg) shouldBeGreaterThanOrEqual 4.5
                }
            }
        }
    })

private fun contrastRatio(foreground: Color, background: Color): Double {
    val lighter = maxOf(relativeLuminance(foreground), relativeLuminance(background))
    val darker = minOf(relativeLuminance(foreground), relativeLuminance(background))
    return (lighter + 0.05) / (darker + 0.05)
}

private fun relativeLuminance(color: Color): Double {
    fun linearize(channel: Float): Double = if (channel <= 0.03928f) {
        channel / 12.92
    } else {
        ((channel + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * linearize(color.red) + 0.7152 * linearize(color.green) + 0.0722 * linearize(color.blue)
}
