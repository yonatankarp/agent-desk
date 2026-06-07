package com.yonatankarp.agentdesk.app.fixtures

import com.yonatankarp.agentdesk.app.operator.verification.ContentDigest
import com.yonatankarp.agentdesk.app.operator.verification.DigestAlgorithm
import com.yonatankarp.agentdesk.app.operator.verification.VerificationInputBinding
import com.yonatankarp.agentdesk.app.operator.verification.VerificationKind
import com.yonatankarp.agentdesk.app.operator.verification.VerificationResult
import com.yonatankarp.agentdesk.app.operator.verification.VerificationState
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.testfixtures.sanitizedNoteEvidence

/**
 * App-local verification fixtures (the `:test-fixtures` DSL only knows `:core`
 * types). Vary only the fields a test pins; `inputBinding` defaults to null so
 * the unbound/conservative path is the explicit, opt-in case.
 */
internal fun verificationResult(
    name: String,
    kind: VerificationKind,
    result: VerificationState,
    durationMillis: Long? = 1_200,
    outputReference: String = "artifact:verification-output",
    failureSummary: String? = null,
    inputBinding: VerificationInputBinding? = null,
): VerificationResult = VerificationResult(
    name = name,
    kind = kind,
    result = result,
    durationMillis = durationMillis,
    outputReference = outputReference,
    failureSummary = failureSummary,
    evidenceReference = sanitizedNoteEvidence("Verification evidence", outputReference),
    inputBinding = inputBinding,
)

internal fun verificationBinding(
    capturedAtMinute: Int,
    digest: String = "a".repeat(64),
): VerificationInputBinding = VerificationInputBinding(
    digest = ContentDigest.parseSha256(digest),
    algorithm = DigestAlgorithm.Sha256,
    capturedAt = EventTimestamp.parse("2026-06-02T21:${capturedAtMinute.toString().padStart(2, '0')}:00Z"),
)
