package com.yonatankarp.agentdesk.app.operator.audit

import com.yonatankarp.agentdesk.app.operator.action.MockActionApprovalState
import com.yonatankarp.agentdesk.app.operator.action.PermissionDecisionState

/**
 * Human-readable labels for the action/audit states rendered on operator
 * surfaces. Presentation only: enum values and their audit wire serialization
 * are owned by [AuditResult] and AuditRecordJson and stay untouched.
 */
object AuditDisplayLabels {
    fun labelFor(result: AuditResult): String = when (result) {
        AuditResult.Approved -> "Approved"
        AuditResult.Rejected -> "Rejected"
        AuditResult.Deferred -> "Deferred"
        AuditResult.RequiresClarification -> "Needs clarification"
        AuditResult.Canceled -> "Canceled"
        AuditResult.Failed -> "Failed"
        AuditResult.PartialSuccess -> "Partial success"
        AuditResult.Unsupported -> "Unsupported"
        AuditResult.Imported -> "Imported"
    }

    fun labelFor(state: MockActionApprovalState): String = when (state) {
        MockActionApprovalState.Approved -> "Approved"
        MockActionApprovalState.Rejected -> "Rejected"
        MockActionApprovalState.Deferred -> "Deferred"
        MockActionApprovalState.Canceled -> "Canceled"
        MockActionApprovalState.Failed -> "Failed"
        MockActionApprovalState.PartialSuccess -> "Partial success"
        MockActionApprovalState.Unsupported -> "Unsupported"
    }

    fun labelFor(state: PermissionDecisionState): String = when (state) {
        PermissionDecisionState.Approved -> "Approved"
        PermissionDecisionState.Denied -> "Denied"
        PermissionDecisionState.Canceled -> "Canceled"
        PermissionDecisionState.RequiresClarification -> "Needs clarification"
        PermissionDecisionState.Unsupported -> "Unsupported"
    }
}
