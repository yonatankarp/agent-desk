package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.app.config.AgentDeskRuntimeConfig
import com.yonatankarp.agentdesk.app.config.AgentDeskRuntimeConfigParser
import com.yonatankarp.agentdesk.app.config.ConfigValidationException
import com.yonatankarp.agentdesk.app.operator.RuntimeConfiguredOperatorStateLoader
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostProfileConfigParser
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostReachabilityChecks
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostReachabilityDiagnostic
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostReachabilityDiagnostics
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostReachabilityException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.Properties

object DesktopRuntimeStateProvider {
    fun load(args: Array<String>): DesktopScreenState = try {
        val runtimeArgs = args.toRuntimeArgs()
            ?: return DesktopScreenState.Error("Usage: agent-desk-desktop [--config <properties-file>] [--host-config <properties-file>]")
        val screenState = DesktopStateResolver(RuntimeConfiguredOperatorStateLoader::load).resolve(runtimeArgs.config)
        if (screenState is DesktopScreenState.Ready && runtimeArgs.hostConfigPath != null) {
            screenState.copy(
                state = screenState.state.copy(hostConnectivity = loadHostConnectivity(runtimeArgs.hostConfigPath)),
            )
        } else {
            screenState
        }
    } catch (error: ConfigValidationException) {
        DesktopScreenState.Error(error.message ?: "Configured operator state could not be loaded.")
    }

    private fun Array<String>.toRuntimeArgs(): DesktopRuntimeArgs? {
        if (isEmpty()) {
            return DesktopRuntimeArgs(config = AgentDeskRuntimeConfig.defaults())
        }

        var configPath: String? = null
        var hostConfigPath: String? = null
        var index = 0
        while (index < size) {
            when (this[index]) {
                "--config" -> {
                    val path = getOrNull(index + 1) ?: return null
                    if (path.startsWith("-") || configPath != null) return null
                    configPath = path
                    index += 1
                }

                "--host-config" -> {
                    val path = getOrNull(index + 1) ?: return null
                    if (path.startsWith("-") || hostConfigPath != null) return null
                    hostConfigPath = path
                    index += 1
                }

                else -> return null
            }
            index += 1
        }

        val config = configPath?.let(::readRuntimeConfig) ?: AgentDeskRuntimeConfig.defaults()
        return DesktopRuntimeArgs(config = config, hostConfigPath = hostConfigPath)
    }

    private fun readRuntimeConfig(path: String): AgentDeskRuntimeConfig = try {
        AgentDeskRuntimeConfigParser.parse(readProperties(path))
    } catch (error: ConfigValidationException) {
        throw error
    } catch (error: IOException) {
        throw ConfigValidationException("config file could not be read")
    } catch (error: InvalidPathException) {
        throw ConfigValidationException("config file could not be read")
    } catch (error: SecurityException) {
        throw ConfigValidationException("config file could not be read")
    }

    private fun loadHostConnectivity(path: String): RuntimeHostReachabilityDiagnostic {
        val profile = try {
            RuntimeHostProfileConfigParser.parse(readProperties(path))
        } catch (error: RuntimeHostReachabilityException) {
            return RuntimeHostReachabilityDiagnostics.notConfigured()
        } catch (error: IOException) {
            throw ConfigValidationException("host config file could not be read")
        } catch (error: InvalidPathException) {
            throw ConfigValidationException("host config file could not be read")
        } catch (error: SecurityException) {
            throw ConfigValidationException("host config file could not be read")
        }
        return RuntimeHostReachabilityChecks.check(profile)
    }

    private fun readProperties(path: String): Map<String, String> {
        val properties = Properties()
        Files.newBufferedReader(Path.of(path)).use(properties::load)
        return properties.entries.associate { (key, value) -> key.toString() to value.toString() }
    }
}

private data class DesktopRuntimeArgs(
    val config: AgentDeskRuntimeConfig,
    val hostConfigPath: String? = null,
)
