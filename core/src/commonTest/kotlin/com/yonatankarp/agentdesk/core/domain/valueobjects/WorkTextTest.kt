package com.yonatankarp.agentdesk.core.domain.valueobjects

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class WorkTextTest :
    BehaviorSpec({
        given("shared single-line work text constraints") {
            `when`("raw text has extra surrounding and internal whitespace") {
                then("it trims and collapses the text") {
                    val text = SingleLineWorkText.parse(
                        raw = "  Review\t  build logs  ",
                        fieldName = "Work text",
                        maxLength = 120,
                    )

                    text.value shouldBe "Review build logs"
                }
            }

            `when`("raw text is blank after trimming") {
                then("it rejects the value") {
                    shouldThrow<IllegalArgumentException> {
                        SingleLineWorkText.parse(
                            raw = "   ",
                            fieldName = "Work text",
                            maxLength = 120,
                        )
                    }
                }
            }

            `when`("raw text spans more than one line") {
                then("it rejects the value") {
                    shouldThrow<IllegalArgumentException> {
                        SingleLineWorkText.parse(
                            raw = "Review logs\nand retry",
                            fieldName = "Work text",
                            maxLength = 120,
                        )
                    }
                }
            }

            `when`("normalized text is at the configured maximum") {
                then("it accepts the value") {
                    val text = SingleLineWorkText.parse(
                        raw = "  ${"x".repeat(120)}  ",
                        fieldName = "Work text",
                        maxLength = 120,
                    )

                    text.value shouldBe "x".repeat(120)
                }
            }

            `when`("normalized text is longer than the configured maximum") {
                then("it rejects the value") {
                    shouldThrow<IllegalArgumentException> {
                        SingleLineWorkText.parse(
                            raw = "x".repeat(121),
                            fieldName = "Work text",
                            maxLength = 120,
                        )
                    }
                }
            }
        }

        given("semantic work text wrappers") {
            `when`("parsing a title and summary") {
                then("they preserve domain names at call sites and normalized values") {
                    val title = WorkItemTitle.parse("  Review   build logs  ")
                    val summary = WorkSummary.parse("  CI failed   on the core test task.  ")

                    title.value shouldBe "Review build logs"
                    title.toString() shouldBe "Review build logs"
                    summary.value shouldBe "CI failed on the core test task."
                    summary.toString() shouldBe "CI failed on the core test task."
                }
            }

            `when`("title and summary input violates shared constraints") {
                then("both wrappers reject blank and multiline values") {
                    shouldThrow<IllegalArgumentException> {
                        WorkItemTitle.parse("   ")
                    }
                    shouldThrow<IllegalArgumentException> {
                        WorkSummary.parse("   ")
                    }
                    shouldThrow<IllegalArgumentException> {
                        WorkItemTitle.parse("Review logs\nand retry")
                    }
                    shouldThrow<IllegalArgumentException> {
                        WorkSummary.parse("Build failed\nSee private log path")
                    }
                }
            }

            `when`("title and summary input exceeds their semantic limits") {
                then("each wrapper applies its own maximum length") {
                    WorkItemTitle.parse("x".repeat(120)).value shouldBe "x".repeat(120)
                    WorkSummary.parse("x".repeat(280)).value shouldBe "x".repeat(280)

                    shouldThrow<IllegalArgumentException> {
                        WorkItemTitle.parse("x".repeat(121))
                    }
                    shouldThrow<IllegalArgumentException> {
                        WorkSummary.parse("x".repeat(281))
                    }
                }
            }

            `when`("title and summary input includes renderer-unsafe public boundary details") {
                then("both wrappers reject the unsafe text without echoing it") {
                    unsafeRendererTextExamples().forEach { unsafe ->
                        val titleError = shouldThrow<IllegalArgumentException> {
                            WorkItemTitle.parse("Review $unsafe")
                        }
                        val summaryError = shouldThrow<IllegalArgumentException> {
                            WorkSummary.parse("Blocked by $unsafe")
                        }

                        titleError.message.orEmpty() shouldContain "Work item title"
                        summaryError.message.orEmpty() shouldContain "Work summary"
                        titleError.message.orEmpty() shouldNotContain unsafe
                        summaryError.message.orEmpty() shouldNotContain unsafe
                    }
                }
            }
        }
    })

private fun unsafeRendererTextExamples(): List<String> {
    val unixPath = "/" + listOf("home", "operator", "workspace", "private.log").joinToString("/")
    val macPath = "/" + listOf("Users", "operator", "Library", "private.log").joinToString("/")
    val windowsPath = listOf("C:", "Users", "operator", "AppData", "private.log").joinToString("\\")
    val privateUrl = "https://" + listOf("localhost", "runtime").joinToString("/")

    return listOf(
        unixPath,
        macPath,
        windowsPath,
        privateUrl,
        "123456789012345678",
        listOf("raw", "transcript excerpt").joinToString(" "),
        listOf("bearer", "credential-marker").joinToString(" "),
        listOf("Open", "Claw runtime context").joinToString(""),
    )
}
