package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.testfixtures.eventTimestampAt
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class StaleDisplayFormatterTest :
    FunSpec({
        test("durations humanize with zero components dropped") {
            StaleDisplayFormatter.humanizeMinutes(0) shouldBe "0m"
            StaleDisplayFormatter.humanizeMinutes(-5) shouldBe "0m"
            StaleDisplayFormatter.humanizeMinutes(45) shouldBe "45m"
            StaleDisplayFormatter.humanizeMinutes(60) shouldBe "1h"
            StaleDisplayFormatter.humanizeMinutes(90) shouldBe "1h 30m"
            StaleDisplayFormatter.humanizeMinutes(120) shouldBe "2h"
            StaleDisplayFormatter.humanizeMinutes(26 * 60) shouldBe "1d 2h"
            StaleDisplayFormatter.humanizeMinutes(3 * 24 * 60) shouldBe "3d"
        }

        test("canonical timestamps humanize to a date, minute precision, and UTC marker") {
            StaleDisplayFormatter.humanizeTimestamp(eventTimestampAt(minute = 0).toString()) shouldBe
                "2026-06-02 21:00 UTC"
            StaleDisplayFormatter.humanizeTimestamp("2026-06-02T21:05:00.123Z") shouldBe
                "2026-06-02 21:05 UTC"
        }

        test("non-canonical timestamps degrade to the raw string instead of throwing") {
            StaleDisplayFormatter.humanizeTimestamp("") shouldBe ""
            StaleDisplayFormatter.humanizeTimestamp("not-a-timestamp") shouldBe "not-a-timestamp"
            StaleDisplayFormatter.humanizeTimestamp("2026-06-02 21:00") shouldBe "2026-06-02 21:00"
        }
    })
