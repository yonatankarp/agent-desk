package com.yonatankarp.agentdesk.app.runtime

import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class RuntimeHostAccessBoundaryTest :
    BehaviorSpec({
        given("runtime host auth state rendering") {
            `when`("each auth state is rendered") {
                then("operators see public-safe state without credentials or endpoints") {
                    val alias = RuntimeHostAlias.parse("host:primary")
                    val boundaries = listOf(
                        RuntimeHostAccessBoundary(
                            authState = RuntimeHostAuthState.NotConfigured,
                            permissionMode = RuntimeHostPermissionMode.Unsupported,
                        ),
                        RuntimeHostAccessBoundary(
                            alias = alias,
                            authState = RuntimeHostAuthState.Pending,
                            permissionMode = RuntimeHostPermissionMode.DiagnosticOnly,
                        ),
                        RuntimeHostAccessBoundary(
                            alias = alias,
                            authState = RuntimeHostAuthState.Accepted,
                            permissionMode = RuntimeHostPermissionMode.ActionCapable,
                        ),
                        RuntimeHostAccessBoundary(
                            alias = alias,
                            authState = RuntimeHostAuthState.Rejected,
                            permissionMode = RuntimeHostPermissionMode.DiagnosticOnly,
                        ),
                        RuntimeHostAccessBoundary(
                            alias = alias,
                            authState = RuntimeHostAuthState.Expired,
                            permissionMode = RuntimeHostPermissionMode.DiagnosticOnly,
                        ),
                        RuntimeHostAccessBoundary(
                            alias = alias,
                            authState = RuntimeHostAuthState.Unsupported,
                            permissionMode = RuntimeHostPermissionMode.Unsupported,
                        ),
                    )

                    boundaries.map { it.authState.wireName }.shouldContainExactly(
                        "not-configured",
                        "pending",
                        "accepted",
                        "rejected",
                        "expired",
                        "unsupported",
                    )
                    boundaries.map { it.publicMessage() }.forEach { message ->
                        message.shouldBePublicSafe()
                        assertSoftly(message) {
                            shouldNotContain(privateEndpoint())
                            shouldNotContain("credential")
                            shouldNotContain("token")
                        }
                    }
                }
            }
        }

        given("runtime host permission modes") {
            `when`("operation permissions are evaluated") {
                then("each mode exposes only its intended operation class") {
                    RuntimeHostPermissionMode.DiagnosticOnly.allowedOperationNames()
                        .shouldContainExactly("reachability-diagnostic")
                    RuntimeHostPermissionMode.ReadOnlyObservation.allowedOperationNames()
                        .shouldContainExactly("reachability-diagnostic", "read-observation")
                    RuntimeHostPermissionMode.ActionCapable.allowedOperationNames()
                        .shouldContainExactly(
                            "reachability-diagnostic",
                            "read-observation",
                            "inspect-action-proposal",
                        )
                    RuntimeHostPermissionMode.Unsupported.allowedOperationNames() shouldBe emptyList()

                    RuntimeHostPermissionMode.ActionCapable.allows(RuntimeHostOperation.MutatingLiveAction) shouldBe false
                }
            }

            `when`("auth is not accepted") {
                then("operations are denied even if the mode lists diagnostic capability") {
                    val boundary = RuntimeHostAccessBoundary(
                        alias = RuntimeHostAlias.parse("host:primary"),
                        authState = RuntimeHostAuthState.Pending,
                        permissionMode = RuntimeHostPermissionMode.DiagnosticOnly,
                    )

                    boundary.allows(RuntimeHostOperation.ReachabilityDiagnostic) shouldBe false
                    boundary.publicMessage().shouldBePublicSafe()
                }
            }

            `when`("auth is accepted") {
                then("allowed operations follow the permission mode") {
                    val boundary = RuntimeHostAccessBoundary(
                        alias = RuntimeHostAlias.parse("host:primary"),
                        authState = RuntimeHostAuthState.Accepted,
                        permissionMode = RuntimeHostPermissionMode.ReadOnlyObservation,
                    )

                    boundary.allows(RuntimeHostOperation.ReachabilityDiagnostic) shouldBe true
                    boundary.allows(RuntimeHostOperation.ReadObservation) shouldBe true
                    boundary.allows(RuntimeHostOperation.InspectActionProposal) shouldBe false
                    boundary.allows(RuntimeHostOperation.MutatingLiveAction) shouldBe false
                }
            }
        }

        given("runtime host credential references") {
            `when`("a local credential reference is parsed") {
                then("string rendering redacts the private value") {
                    val credential = RuntimeHostCredentialReference.parse(privateCredentialReference())

                    credential.value shouldBe privateCredentialReference()
                    credential.toString() shouldBe "<local-host-credential-reference>"
                    credential.toString().shouldBePublicSafe()
                }
            }

            `when`("credential reference input contains secret-like values") {
                then("validation rejects it without echoing the raw value") {
                    unsafeCredentialReferences().forEach { unsafe ->
                        val error = shouldThrow<RuntimeHostReachabilityException> {
                            RuntimeHostCredentialReference.parse(unsafe)
                        }

                        assertSoftly(error.message.orEmpty()) {
                            shouldContain("hostCredentialReference")
                            shouldNotContain(unsafe)
                            shouldNotContain(rawIdentifier())
                        }
                    }
                }
            }
        }

        given("boundary consistency rules") {
            `when`("invalid auth and permission combinations are built") {
                then("construction fails before public rendering") {
                    shouldThrow<RuntimeHostReachabilityException> {
                        RuntimeHostAccessBoundary(
                            alias = RuntimeHostAlias.parse("host:primary"),
                            authState = RuntimeHostAuthState.NotConfigured,
                            permissionMode = RuntimeHostPermissionMode.Unsupported,
                        )
                    }.message shouldBe "not-configured host auth must not include an alias"

                    shouldThrow<RuntimeHostReachabilityException> {
                        RuntimeHostAccessBoundary(
                            authState = RuntimeHostAuthState.Accepted,
                            permissionMode = RuntimeHostPermissionMode.DiagnosticOnly,
                        )
                    }.message shouldBe "accepted host auth requires an alias"

                    shouldThrow<RuntimeHostReachabilityException> {
                        RuntimeHostAccessBoundary(
                            alias = RuntimeHostAlias.parse("host:primary"),
                            authState = RuntimeHostAuthState.Pending,
                            permissionMode = RuntimeHostPermissionMode.ActionCapable,
                        )
                    }.message shouldBe "action-capable mode requires accepted host auth"
                }
            }
        }
    })

private fun privateEndpoint(): String = "https://" + listOf("agent", "desk", "host").joinToString(".")

private fun privateCredentialReference(): String = listOf("local", "host", "credential").joinToString("-")

private fun rawIdentifier(): String = "123456789" + "012345678"

private fun unsafeCredentialReferences(): List<String> = listOf(
    "auth_token=value",
    "bearer credential-marker",
    "channel:${rawIdentifier()}",
)
