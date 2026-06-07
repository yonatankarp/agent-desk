package com.yonatankarp.agentdesk.mobile

import androidx.compose.ui.graphics.Color
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import kotlin.math.pow

/**
 * Drift pin for the WCAG AA small-text contrast decision (#318): muted text
 * renders at 10-12sp on every light background, so the token must keep a
 * >= 4.5:1 ratio against all of them or accessibility silently regresses.
 */
class MobilePaletteContrastTest :
    FunSpec({
        test("muted small text meets WCAG AA on every background it sits on") {
            listOf(MobilePalette.Row, MobilePalette.Surface, MobilePalette.Background).forEach { background ->
                contrastRatio(MobilePalette.TextMuted, background) shouldBeGreaterThanOrEqual 4.5
            }
        }
    })

private fun contrastRatio(
    foreground: Color,
    background: Color,
): Double {
    val lighter = maxOf(relativeLuminance(foreground), relativeLuminance(background))
    val darker = minOf(relativeLuminance(foreground), relativeLuminance(background))
    return (lighter + 0.05) / (darker + 0.05)
}

private fun relativeLuminance(color: Color): Double {
    fun linearize(channel: Float): Double = if (channel <= 0.03928f) {
        channel / 12.92
    } else {
        ((channel + 0.055) / 1.055).toDouble().pow(2.4)
    }
    return 0.2126 * linearize(color.red) + 0.7152 * linearize(color.green) + 0.0722 * linearize(color.blue)
}
