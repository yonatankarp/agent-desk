package com.yonatankarp.agentdesk.testfixtures.matchers

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty

/**
 * Canonical public-safety denylist. Union of the per-module lists this
 * matcher replaced (CLI, app privacy regression, desktop/mobile inline).
 * Matched case-insensitively.
 */
private val denyTerms: List<String> =
    listOf(
        "/home/",
        "/users/",
        "\\users\\",
        "c:\\",
        "file:",
        "localhost",
        "private-token",
        "auth_token",
        "github_pat_",
        "ghp_",
        "xoxb-",
        "bearer",
        "password",
        "secret",
        "token",
        "op:" + "//",
        "discord",
        "channel:",
        "message:",
        "session:",
        "thread:",
        "raw transcript",
    )

private val rawIdentifier: String = "123456789" + "012345678"

/** Asserts the text contains none of the canonical public-safety denylist terms. */
fun String.shouldBePublicSafe() {
    val lowered = lowercase()
    val violations =
        denyTerms.filter { it in lowered } +
            listOfNotNull(rawIdentifier.takeIf { it in this })

    withClue("Expected public-safe text but found denylisted content $violations in: $this") {
        violations.shouldBeEmpty()
    }
}
