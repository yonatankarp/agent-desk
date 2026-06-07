package com.yonatankarp.agentdesk.app.operator

/**
 * Shared presentation for stale-attention values: humanized durations and
 * operator-friendly timestamps. Lives in `:app` so every client renders the
 * same vocabulary — both the mobile shell and the CLI operator console derive
 * stale durations through it.
 */
object StaleDisplayFormatter {
    private const val MINUTES_PER_HOUR = 60L
    private const val MINUTES_PER_DAY = 24L * MINUTES_PER_HOUR

    private val canonicalUtcTimestamp = """\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2}(?:\.\d+)?)?Z""".toRegex()

    fun humanizeMinutes(minutes: Long): String {
        if (minutes <= 0) {
            return "0m"
        }
        val days = minutes / MINUTES_PER_DAY
        val hours = minutes % MINUTES_PER_DAY / MINUTES_PER_HOUR
        val remainingMinutes = minutes % MINUTES_PER_HOUR
        val components = when {
            days > 0 -> listOf("${days}d", "${hours}h".takeIf { hours > 0 })
            hours > 0 -> listOf("${hours}h", "${remainingMinutes}m".takeIf { remainingMinutes > 0 })
            else -> listOf("${remainingMinutes}m")
        }
        return components.filterNotNull().joinToString(" ")
    }

    /**
     * Canonical UTC instants render as "YYYY-MM-DD HH:mm UTC"; anything else
     * degrades to the raw string so an unexpected input never crashes a render.
     */
    fun humanizeTimestamp(timestamp: String): String = if (canonicalUtcTimestamp.matches(timestamp)) {
        "${timestamp.take(10)} ${timestamp.substring(11, 16)} UTC"
    } else {
        timestamp
    }
}
