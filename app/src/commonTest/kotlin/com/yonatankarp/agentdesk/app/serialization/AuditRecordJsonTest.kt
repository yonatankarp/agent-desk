package com.yonatankarp.agentdesk.app.serialization

import com.yonatankarp.agentdesk.app.fixtures.auditEntry
import com.yonatankarp.agentdesk.app.operator.audit.AuditActorKind
import com.yonatankarp.agentdesk.app.operator.audit.AuditResult
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class AuditRecordJsonTest :
    BehaviorSpec({
        given("audit entries across every result and actor kind") {
            `when`("each entry is encoded and decoded") {
                then("the round trip preserves the entry") {
                    AuditResult.entries.forEach { result ->
                        AuditActorKind.entries.forEach { actorKind ->
                            val entry = auditEntry(result = result, actorKind = actorKind)

                            AuditRecordJson.decode(AuditRecordJson.encode(entry)) shouldBe entry
                        }
                    }
                }

                then("the encoded record is public-safe") {
                    AuditRecordJson.encode(auditEntry()).shouldBePublicSafe()
                }
            }

            `when`("a timestamp is written at non-canonical precision") {
                then("the wire form carries the canonical value and round-trips") {
                    val entry = auditEntry().copy(
                        timestamp = EventTimestamp.parse("2026-06-02T21:22:00.500Z"),
                    )

                    val encoded = AuditRecordJson.encode(entry)

                    encoded shouldContain "2026-06-02T21:22:00.5Z"
                    AuditRecordJson.decode(encoded).timestamp shouldBe EventTimestamp.parse("2026-06-02T21:22:00.5Z")
                }
            }
        }

        given("a hand-edited store line") {
            `when`("a field smuggles non-public-safe content") {
                then("decode rejects it without echoing the content") {
                    val privatePath = "/" + "home/user/private.log"
                    val tampered = AuditRecordJson.encode(auditEntry())
                        .replace("Public-safe mock approval.", "Read $privatePath")

                    val error = shouldThrow<IllegalArgumentException> {
                        AuditRecordJson.decode(tampered)
                    }

                    error.message.orEmpty() shouldNotContain privatePath
                }
            }

            `when`("the audit id is malformed") {
                then("decode rejects it through the typed id parser") {
                    val tampered = AuditRecordJson.encode(auditEntry())
                        .replace("\"id\":\"audit:", "\"id\":\"not-audit:")

                    val error = shouldThrow<IllegalArgumentException> {
                        AuditRecordJson.decode(tampered)
                    }

                    error.message.orEmpty() shouldContain "Audit entry id"
                }
            }

            `when`("the result carries an unknown wire name") {
                then("decode rejects it") {
                    val tampered = AuditRecordJson.encode(auditEntry(result = AuditResult.Rejected))
                        .replace("\"rejected\"", "\"exploded\"")

                    shouldThrow<IllegalArgumentException> {
                        AuditRecordJson.decode(tampered)
                    }
                }
            }
        }
    })
