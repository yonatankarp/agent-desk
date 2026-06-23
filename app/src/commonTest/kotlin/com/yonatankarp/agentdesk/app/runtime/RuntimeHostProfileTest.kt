package com.yonatankarp.agentdesk.app.runtime

import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class RuntimeHostProfileTest :
    BehaviorSpec({
        given("a local runtime host profile") {
            `when`("valid local-only host settings are parsed") {
                then("it keeps endpoint details local and renders only the public alias") {
                    val profile = RuntimeHostProfileConfigParser.parse(
                        mapOf(
                            "hostAlias" to "operator-lab",
                            "hostEndpoint" to privateHttpEndpoint(),
                            "hostAliasMappings" to "runtime-primary=host:primary,runtime-backup=operator-backup",
                            "hostAuthState" to "accepted",
                            "hostPermissionMode" to "read-only-observation",
                            "hostObservationBridge" to "sanitized-observations.json",
                        ),
                    )

                    profile.alias.value shouldBe "operator-lab"
                    profile.endpoint.value shouldBe privateHttpEndpoint()
                    profile.aliasMappings.map { it.alias.value }.shouldContainExactly(
                        "host:primary",
                        "operator-backup",
                    )
                    profile.aliasMappings.map { it.runtimeHostId.value }.shouldContainExactly(
                        "runtime-primary",
                        "runtime-backup",
                    )
                    profile.authState shouldBe RuntimeHostAuthState.Accepted
                    profile.permissionMode shouldBe RuntimeHostPermissionMode.ReadOnlyObservation
                    profile.observationBridge.toString() shouldBe "<local-host-observation-bridge>"
                    profile.accessBoundary().allows(RuntimeHostOperation.ReadObservation) shouldBe true
                    profile.toString().shouldBePublicSafe()
                    profile.aliasMappings.joinToString().shouldBePublicSafe()
                    profile.publicSummary().shouldBePublicSafe()
                    assertSoftly(profile.publicSummary()) {
                        shouldContain("host=operator-lab")
                        shouldContain("endpoint=local-only")
                        shouldNotContain(privateHost())
                        shouldNotContain("8443")
                    }
                    assertSoftly(profile.toString()) {
                        shouldNotContain(privateHost())
                        shouldNotContain("8443")
                        shouldNotContain("runtime-primary")
                        shouldNotContain("sanitized-observations.json")
                    }
                }
            }

            `when`("required settings are missing") {
                then("validation names the missing field without endpoint details") {
                    val aliasError = shouldThrow<RuntimeHostReachabilityException> {
                        RuntimeHostProfileConfigParser.parse(
                            mapOf("hostEndpoint" to privateHttpEndpoint()),
                        )
                    }
                    val endpointError = shouldThrow<RuntimeHostReachabilityException> {
                        RuntimeHostProfileConfigParser.parse(
                            mapOf("hostAlias" to "operator-lab"),
                        )
                    }

                    aliasError.message shouldBe "hostAlias must be configured"
                    endpointError.message shouldBe "hostEndpoint must be configured"
                }
            }

            `when`("local endpoint configuration is malformed or unsafe") {
                then("validation rejects it without echoing the raw value") {
                    unsafeEndpoints().forEach { unsafe ->
                        val error = shouldThrow<RuntimeHostReachabilityException> {
                            RuntimeHostEndpoint.parse(unsafe)
                        }

                        assertSoftly(error.message.orEmpty()) {
                            shouldContain("hostEndpoint")
                            if (unsafe.isNotBlank()) {
                                shouldNotContain(unsafe)
                            }
                            shouldNotContain(privateHost())
                            shouldNotContain(rawIdentifier())
                        }
                    }
                }
            }

            `when`("alias mappings are malformed or unsafe") {
                then("validation rejects them before publishing public state") {
                    val malformed = shouldThrow<RuntimeHostReachabilityException> {
                        RuntimeHostProfileConfigParser.parse(
                            mapOf(
                                "hostAlias" to "operator-lab",
                                "hostEndpoint" to privateHttpEndpoint(),
                                "hostAliasMappings" to "runtime-primary",
                            ),
                        )
                    }
                    malformed.message shouldBe "hostAliasMappings entries must use runtimeHostId=alias"

                    unsafeRuntimeIds().forEach { unsafe ->
                        val error = shouldThrow<RuntimeHostReachabilityException> {
                            RuntimeHostPrivateId.parse(unsafe)
                        }

                        assertSoftly(error.message.orEmpty()) {
                            shouldContain("runtimeHostId")
                            shouldNotContain(unsafe)
                            shouldNotContain(rawIdentifier())
                        }
                    }
                }
            }

            `when`("sync fields are unsupported or unsafe") {
                then("validation rejects them without echoing private values") {
                    shouldThrow<RuntimeHostReachabilityException> {
                        RuntimeHostAuthState.parse("approved")
                    }.message shouldBe "hostAuthState is not supported"

                    shouldThrow<RuntimeHostReachabilityException> {
                        RuntimeHostPermissionMode.parse("control")
                    }.message shouldBe "hostPermissionMode is not supported"

                    unsafeObservationBridgeValues().forEach { unsafe ->
                        val error = shouldThrow<RuntimeHostReachabilityException> {
                            RuntimeHostObservationBridge.parse(unsafe)
                        }

                        assertSoftly(error.message.orEmpty()) {
                            shouldContain("hostObservationBridge")
                            shouldNotContain(unsafe)
                            shouldNotContain(rawIdentifier())
                        }
                    }
                }
            }
        }
    })

private fun privateHost(): String = listOf("agent", "desk", "host").joinToString(".")

private fun privateHttpEndpoint(): String = "https://${privateHost()}:8443/status"

private fun rawIdentifier(): String = "123456789" + "012345678"

private fun unsafeEndpoints(): List<String> = listOf(
    "",
    "agent-desk-host",
    "ftp://${privateHost()}",
    "https://",
    "https://operator:${"private"}@${privateHost()}",
    "https://${privateHost()}/token",
    "https://${privateHost()}\nnext",
    "https://${privateHost()}/${rawIdentifier()}",
)

private fun unsafeRuntimeIds(): List<String> = listOf(
    "runtime=https://${privateHost()}",
    "auth_token=value",
    "channel:${rawIdentifier()}",
)

private fun unsafeObservationBridgeValues(): List<String> = listOf(
    "auth_token=value",
    "channel:${rawIdentifier()}",
    "raw transcript export",
)
