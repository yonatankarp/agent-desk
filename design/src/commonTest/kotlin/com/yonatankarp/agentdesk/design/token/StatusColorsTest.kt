package com.yonatankarp.agentdesk.design.token

import com.yonatankarp.agentdesk.app.operator.StatusTone
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldNotBe

/**
 * Real failure modes: (1) every StatusTone must resolve in both schemes (a new
 * enum value would otherwise crash at render), (2) the light-mode Attention vs
 * Blocked rails must stay visually distinct — the bug fixed by hand in the
 * brainstorm where amber#B45309 read as red next to red#DC2626.
 */
class StatusColorsTest :
    FunSpec({
        test("every tone resolves in light and dark") {
            StatusTone.entries.forEach { tone ->
                StatusColors.Light.forTone(tone) shouldNotBe null
                StatusColors.Dark.forTone(tone) shouldNotBe null
            }
        }

        test("light Attention rail is distinct from Blocked rail") {
            val attention = StatusColors.Light.forTone(StatusTone.Attention).rail
            val blocked = StatusColors.Light.forTone(StatusTone.Blocked).rail
            attention shouldNotBe blocked
            // Hue separation guard: amber rail must be visibly more green than red.
            (attention.green - attention.red) shouldBeAtLeastDelta (blocked.green - blocked.red)
        }
    })

private infix fun Float.shouldBeAtLeastDelta(other: Float) {
    require(this > other + 0.15f) {
        "expected amber rail to be clearly distinct from red rail (got $this vs $other)"
    }
}
