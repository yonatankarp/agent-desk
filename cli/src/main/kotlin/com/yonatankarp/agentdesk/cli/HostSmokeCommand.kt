package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.app.runtime.RuntimeHostProfile
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostProfileConfigParser
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostReachabilityDiagnostic
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostReachabilityDiagnostics
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostReachabilityException
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostReachabilityState
import com.yonatankarp.agentdesk.cli.input.CliInputException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.Properties

object HostSmokeCommand {
    fun execute(
        hostConfigPath: String,
        check: (RuntimeHostProfile) -> RuntimeHostReachabilityDiagnostic,
    ): HostSmokeResult {
        val profile = try {
            RuntimeHostProfileConfigParser.parse(readConfig(hostConfigPath))
        } catch (exception: RuntimeHostReachabilityException) {
            return HostSmokeResult(RuntimeHostReachabilityDiagnostics.notConfigured())
        }
        val diagnostic = check(profile)
        return HostSmokeResult(diagnostic)
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

data class HostSmokeResult(
    val diagnostic: RuntimeHostReachabilityDiagnostic,
) {
    val exitCode: Int = if (diagnostic.state == RuntimeHostReachabilityState.Reachable) 0 else 1
    val text: String = diagnostic.publicMessage()
}
