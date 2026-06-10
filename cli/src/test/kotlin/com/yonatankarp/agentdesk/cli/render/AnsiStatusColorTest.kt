package com.yonatankarp.agentdesk.cli.render

import com.yonatankarp.agentdesk.app.operator.StatusTone
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class AnsiStatusColorTest :
    FunSpec({
        test("colorize wraps text in the tone's truecolor escape when enabled") {
            val out = AnsiStatusColor(enabled = true).colorize("Blocked", StatusTone.Blocked)
            out shouldContain "[38;2;"
            out shouldContain "Blocked"
            out shouldContain "[0m"
        }

        test("colorize is a no-op when disabled (NO_COLOR / non-TTY)") {
            AnsiStatusColor(enabled = false).colorize("Blocked", StatusTone.Blocked) shouldBe "Blocked"
        }

        test("disabled output contains no escape codes") {
            AnsiStatusColor(enabled = false).colorize("Active", StatusTone.Active) shouldNotContain "["
        }

        test("every tone maps to an rgb triple") {
            StatusTone.entries.forEach { tone ->
                AnsiStatusColor(enabled = true).colorize("x", tone) shouldContain "[38;2;"
            }
        }
    })
