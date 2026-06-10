package com.yonatankarp.agentdesk.testfixtures.matchers

import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeFalse

/**
 * Side-effecting action affordances must never render on a read-only operator surface. Match each verb on
 * word boundaries (not as a bare substring) so a completion label such as "Canceled outcome" or an audit
 * label such as "Approved" does not false-positive while a real "Cancel"/"Approve" affordance still trips.
 *
 * Shared by the desktop and mobile read-only smoke snapshots so the two surfaces enforce one denylist.
 */
private val actionVerbs = listOf("Resume", "Approve", "Stop", "Retry", "Cancel")

fun String.shouldHaveNoActionAffordances() {
    actionVerbs.forEach { verb ->
        withClue("read-only render must not expose the '$verb' action affordance") {
            "\\b$verb\\b".toRegex().containsMatchIn(this).shouldBeFalse()
        }
    }
}
