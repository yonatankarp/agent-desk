package com.yonatankarp.agentdesk.testfixtures

import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp

/**
 * Deterministic timestamp on the canonical fixture day (2026-06-02).
 * Defaults to the canonical fixture hour so `eventTimestampAt(minute = 5)`
 * lands shortly after [WorkEventFixtures.startedAt].
 */
fun eventTimestampAt(
    minute: Int,
    hour: Int = 21,
    second: Int = 0,
): EventTimestamp {
    require(hour in 0..23) { "hour must be 0-23" }
    require(minute in 0..59) { "minute must be 0-59" }
    require(second in 0..59) { "second must be 0-59" }
    val hh = hour.toString().padStart(2, '0')
    val mm = minute.toString().padStart(2, '0')
    val ss = second.toString().padStart(2, '0')
    return EventTimestamp.parse("2026-06-02T$hh:$mm:${ss}Z")
}
