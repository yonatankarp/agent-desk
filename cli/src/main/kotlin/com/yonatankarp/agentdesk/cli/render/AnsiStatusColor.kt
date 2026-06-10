package com.yonatankarp.agentdesk.cli.render

import com.yonatankarp.agentdesk.app.operator.StatusTone

/**
 * Maps the shared StatusTone semantics to ANSI truecolor. Disabled (no escapes)
 * when NO_COLOR is set or stdout is not a TTY, so piped/captured output stays clean.
 */
class AnsiStatusColor(private val enabled: Boolean) {
    fun colorize(text: String, tone: StatusTone): String {
        if (!enabled) return text
        val (r, g, b) = rgb(tone)
        return "[38;2;$r;$g;${b}m$text[0m"
    }

    private fun rgb(tone: StatusTone): Triple<Int, Int, Int> = when (tone) {
        StatusTone.Neutral -> Triple(148, 163, 184)
        StatusTone.Active -> Triple(45, 212, 191)
        StatusTone.Attention -> Triple(240, 140, 0)
        StatusTone.Blocked -> Triple(220, 38, 38)
        StatusTone.Success -> Triple(34, 197, 94)
        StatusTone.Failure -> Triple(239, 68, 68)
    }

    companion object {
        fun fromEnvironment(
            isTty: Boolean,
            noColor: String? = System.getenv("NO_COLOR"),
        ): AnsiStatusColor = AnsiStatusColor(enabled = isTty && noColor == null)
    }
}
