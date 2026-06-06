package com.yonatankarp.agentdesk.testfixtures.matchers

import com.yonatankarp.agentdesk.core.domain.projections.OperatorStateProjection
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeEmpty

/** Asserts the projection accepted nothing: no items, no events, no ignored issues. */
fun OperatorStateProjection.shouldBeEmptyProjection() {
    assertSoftly(this) {
        workItems.shouldBeEmpty()
        recentEvents.shouldBeEmpty()
        ignoredEvents.shouldBeEmpty()
    }
}
