package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.app.runtime.RuntimeHostAuthState
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostOperation
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostProfile
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostProfileConfigParser
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostReachabilityDiagnostic
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostReachabilityDiagnostics
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostReachabilityException
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostReachabilityState
import com.yonatankarp.agentdesk.app.runtime.RuntimeWorkEventImportResult
import com.yonatankarp.agentdesk.app.runtime.summary
import com.yonatankarp.agentdesk.cli.input.CliInputException
import com.yonatankarp.agentdesk.cli.io.importOpenClawObservations
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.Properties

object LiveObservationSyncCommand {
    fun execute(
        hostConfigPath: String,
        eventStorePath: String,
        check: (RuntimeHostProfile) -> RuntimeHostReachabilityDiagnostic,
        now: EventTimestamp,
    ): LiveObservationSyncResult {
        val profile = try {
            RuntimeHostProfileConfigParser.parse(readConfig(hostConfigPath))
        } catch (exception: RuntimeHostReachabilityException) {
            return LiveObservationSyncResult.notConfigured()
        }

        val reachability = check(profile)
        if (reachability.state != RuntimeHostReachabilityState.Reachable) {
            return LiveObservationSyncResult.reachabilityFailure(reachability)
        }

        val boundary = profile.accessBoundary()
        if (!boundary.allows(RuntimeHostOperation.ReadObservation)) {
            return LiveObservationSyncResult.accessDenied(profile, boundary.publicMessage())
        }

        val bridge = profile.observationBridge
            ?: return LiveObservationSyncResult.stale(profile, "observation-source-unavailable", null)

        val importResult = try {
            importOpenClawObservations(
                observationsPath = bridge.value,
                eventStorePath = eventStorePath,
            )
        } catch (exception: CliInputException) {
            return LiveObservationSyncResult.unsafeRejected(profile, exception.publicMessage)
        }

        return if (importResult.importedCount + importResult.skippedDuplicateCount == 0) {
            LiveObservationSyncResult.stale(profile, "no-observations", importResult)
        } else {
            LiveObservationSyncResult.synced(profile, importResult, now)
        }
    }

    private fun readConfig(path: String): Map<String, String> {
        val properties = Properties()
        try {
            Files.newInputStream(Path.of(path)).use(properties::load)
        } catch (exception: IOException) {
            throw CliInputException("Host config file could not be read.")
        } catch (exception: InvalidPathException) {
            throw CliInputException("Host config file could not be read.")
        } catch (exception: IllegalArgumentException) {
            throw CliInputException("Host config file could not be read.")
        } catch (exception: SecurityException) {
            throw CliInputException("Host config file could not be read.")
        }

        return properties.stringPropertyNames().associateWith(properties::getProperty)
    }
}

data class LiveObservationSyncResult(
    val exitCode: Int,
    val text: String,
) {
    companion object {
        fun synced(
            profile: RuntimeHostProfile,
            importResult: RuntimeWorkEventImportResult,
            now: EventTimestamp,
        ): LiveObservationSyncResult = LiveObservationSyncResult(
            exitCode = 0,
            text = buildString {
                appendLine("Live observation sync: host=${profile.alias.value} state=synced freshness=fresh last-successful-sync=$now.")
                appendLine(
                    "Imported ${importResult.importedCount} live observation event(s); " +
                        "skipped ${importResult.skippedDuplicateCount} duplicate event(s).",
                )
                append(importResult.diagnostics.summary().publicMessage())
            },
        )

        fun stale(
            profile: RuntimeHostProfile,
            failure: String,
            importResult: RuntimeWorkEventImportResult?,
        ): LiveObservationSyncResult = LiveObservationSyncResult(
            exitCode = 1,
            text = buildString {
                appendLine(
                    "Live observation sync: host=${profile.alias.value} state=stale " +
                        "failure=$failure last-successful-sync=unavailable.",
                )
                importResult?.let {
                    append(it.diagnostics.summary().publicMessage())
                }
            }.trimEnd(),
        )

        fun reachabilityFailure(diagnostic: RuntimeHostReachabilityDiagnostic): LiveObservationSyncResult {
            val host = diagnostic.alias?.value ?: "not-configured"
            val failure = diagnostic.failure?.wireName ?: "host-unavailable"
            return LiveObservationSyncResult(
                exitCode = 1,
                text = buildString {
                    appendLine(diagnostic.publicMessage())
                    append(
                        "Live observation sync: host=$host state=${diagnostic.state.wireName} " +
                            "failure=$failure last-successful-sync=unavailable.",
                    )
                },
            )
        }

        fun accessDenied(
            profile: RuntimeHostProfile,
            accessMessage: String,
        ): LiveObservationSyncResult {
            val failure = when (profile.authState) {
                RuntimeHostAuthState.Rejected -> "authentication-rejected"
                RuntimeHostAuthState.Expired -> "authentication-expired"
                RuntimeHostAuthState.Pending -> "authentication-pending"
                RuntimeHostAuthState.Unsupported -> "authentication-unsupported"
                RuntimeHostAuthState.NotConfigured -> "authentication-not-configured"
                RuntimeHostAuthState.Accepted -> "read-observation-not-allowed"
            }
            return LiveObservationSyncResult(
                exitCode = 1,
                text = buildString {
                    appendLine(accessMessage)
                    append(
                        "Live observation sync: host=${profile.alias.value} state=rejected " +
                            "failure=$failure last-successful-sync=unavailable.",
                    )
                },
            )
        }

        fun unsafeRejected(
            profile: RuntimeHostProfile,
            message: String,
        ): LiveObservationSyncResult = LiveObservationSyncResult(
            exitCode = 1,
            text = buildString {
                appendLine(
                    "Live observation sync: host=${profile.alias.value} state=unsafe-rejected " +
                        "failure=unsafe-payload-rejected last-successful-sync=unavailable.",
                )
                append("Diagnostic: ${message.ifBlank { "Runtime observations could not be imported." }}")
            },
        )

        fun notConfigured(): LiveObservationSyncResult = LiveObservationSyncResult(
            exitCode = 1,
            text = buildString {
                appendLine(RuntimeHostReachabilityDiagnostics.notConfigured().publicMessage())
                append(
                    "Live observation sync: host=not-configured state=not-configured " +
                        "failure=missing-configuration last-successful-sync=unavailable.",
                )
            },
        )
    }
}
