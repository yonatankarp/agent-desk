package com.yonatankarp.agentdesk.app.operator.verification

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotMatch

class CompletionReadinessLabelsTest :
    FunSpec({
        test("readiness states render sentence-case labels instead of enum tokens") {
            CompletionReadinessLabels.labelFor(CompletionReadinessState.Ready) shouldBe "Ready"
            CompletionReadinessLabels.labelFor(CompletionReadinessState.NotReady) shouldBe "Not ready"
            CompletionReadinessLabels.labelFor(CompletionReadinessState.Blocked) shouldBe "Blocked"
            CompletionReadinessLabels.labelFor(CompletionReadinessState.Unknown) shouldBe "Unknown"
        }

        test("no state leaks a camel-case enum token, including future additions") {
            val camelCaseToken = ".*[a-z][A-Z].*"
            CompletionReadinessState.entries.forEach { state ->
                CompletionReadinessLabels.labelFor(state) shouldNotMatch camelCaseToken
            }
        }
    })
