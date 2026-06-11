package com.yonatankarp.agentdesk.design.token

import androidx.compose.ui.graphics.Color
import com.yonatankarp.agentdesk.app.operator.StatusTone
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import kotlin.math.pow

/**
 * Drift pin for WCAG AA. Muted text renders small (10-13sp) so needs >= 4.5:1
 * on every surface it sits on; status pill text sits on its own pillBg and must
 * also clear 4.5:1. Small mobile status/accent text renders at 10-12sp, so all
 * StatusTone text colors plus Accent are pinned on Background, Panel, and Row.
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

        test("small mobile status and accent text meet AA on mobile surfaces, both schemes") {
            listOf(
                AgentDeskColors.Light to StatusColors.Light,
                AgentDeskColors.Dark to StatusColors.Dark,
            ).forEach { (colors, statusColors) ->
                val backgrounds =
                    mapOf(
                        "Background" to colors.background,
                        "Panel" to colors.panel,
                        "Row" to colors.row,
                    )
                val foregrounds =
                    StatusTone.entries.associate { tone -> tone.name to statusColors.forTone(tone).text } +
                        mapOf("Accent" to colors.accent)

                foregrounds.forEach { (name, foreground) ->
                    backgrounds.forEach { (surface, background) ->
                        withClue("$name on $surface") {
                            contrastRatio(foreground, background) shouldBeGreaterThanOrEqual 4.5
                        }
                    }
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
