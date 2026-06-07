package com.yonatankarp.agentdesk.app.operator.audit

import com.yonatankarp.agentdesk.app.operator.action.MockActionApprovalState
import com.yonatankarp.agentdesk.app.operator.action.PermissionDecisionState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotMatch

class AuditDisplayLabelsTest :
    FunSpec({
        test("multi-word states render sentence-case labels instead of enum tokens") {
            AuditDisplayLabels.labelFor(AuditResult.PartialSuccess) shouldBe "Partial success"
            AuditDisplayLabels.labelFor(AuditResult.RequiresClarification) shouldBe "Needs clarification"
            AuditDisplayLabels.labelFor(MockActionApprovalState.PartialSuccess) shouldBe "Partial success"
            AuditDisplayLabels.labelFor(PermissionDecisionState.RequiresClarification) shouldBe "Needs clarification"
        }

        test("the gate's denial keeps its own vocabulary instead of the audit re-coding") {
            AuditDisplayLabels.labelFor(PermissionDecisionState.Denied) shouldBe "Denied"
        }

        test("no state leaks a camel-case enum token, including future additions") {
            val camelCaseToken = ".*[a-z][A-Z].*"
            AuditResult.entries.forEach { AuditDisplayLabels.labelFor(it) shouldNotMatch camelCaseToken }
            MockActionApprovalState.entries.forEach { AuditDisplayLabels.labelFor(it) shouldNotMatch camelCaseToken }
            PermissionDecisionState.entries.forEach { AuditDisplayLabels.labelFor(it) shouldNotMatch camelCaseToken }
        }
    })
