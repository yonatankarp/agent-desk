package com.yonatankarp.agentdesk.mobile

import com.yonatankarp.agentdesk.app.config.AgentDeskRuntimeConfig
import com.yonatankarp.agentdesk.app.config.AgentDeskRuntimeConfigParser
import com.yonatankarp.agentdesk.app.config.ConfigValidationException
import com.yonatankarp.agentdesk.app.operator.RuntimeConfiguredOperatorStateLoadException
import com.yonatankarp.agentdesk.app.operator.RuntimeConfiguredOperatorStateLoader
import com.yonatankarp.agentdesk.app.operator.mobile.MobileOperatorState
import com.yonatankarp.agentdesk.app.operator.mobile.MobileOperatorStateContract
import com.yonatankarp.agentdesk.app.operator.mobile.MobileProjectionWarning
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.Properties

object MobileRuntimeStateProvider {
    fun load(args: Array<String>): MobileOperatorState {
        val config = when (val configArg = args.toConfigArg()) {
            MobileConfigArg.Default -> AgentDeskRuntimeConfig.defaults()

            MobileConfigArg.Invalid -> return warningState("Usage: agent-desk-mobile [--config <properties-file>]")

            is MobileConfigArg.Path -> try {
                readRuntimeConfig(configArg.value)
            } catch (error: ConfigValidationException) {
                return warningState(error.message ?: "config file could not be read")
            }
        }

        return try {
            val operatorState = RuntimeConfiguredOperatorStateLoader.load(config)
            MobileOperatorStateContract.fromState(
                state = operatorState,
                projectionWarnings = operatorState.storeReadWarning
                    ?.let { warning -> listOf(MobileProjectionWarning(eventId = "configured-event-store", reason = warning)) }
                    .orEmpty(),
            )
        } catch (error: RuntimeConfiguredOperatorStateLoadException) {
            warningState(error.message ?: "Configured operator state could not be loaded.")
        }
    }

    private fun Array<String>.toConfigArg(): MobileConfigArg {
        if (isEmpty()) {
            return MobileConfigArg.Default
        }
        if (size != 2 || this[0] != "--config") {
            return MobileConfigArg.Invalid
        }
        val path = this[1]
        if (path.startsWith("-")) {
            return MobileConfigArg.Invalid
        }
        return MobileConfigArg.Path(path)
    }

    private fun readRuntimeConfig(path: String): AgentDeskRuntimeConfig = try {
        AgentDeskRuntimeConfigParser.parse(readProperties(path))
    } catch (error: ConfigValidationException) {
        throw error
    } catch (error: IOException) {
        throw ConfigValidationException("config file could not be read")
    } catch (error: InvalidPathException) {
        throw ConfigValidationException("config file could not be read")
    } catch (error: IllegalArgumentException) {
        throw ConfigValidationException("config file could not be read")
    } catch (error: SecurityException) {
        throw ConfigValidationException("config file could not be read")
    }

    private fun readProperties(path: String): Map<String, String> {
        val properties = Properties()
        Files.newBufferedReader(Path.of(path)).use(properties::load)
        return properties.entries.associate { (key, value) -> key.toString() to value.toString() }
    }

    private fun warningState(message: String): MobileOperatorState = MobileOperatorState(
        currentWork = emptyList(),
        attentionQueue = emptyList(),
        recentEvents = emptyList(),
        projectionWarnings = listOf(MobileProjectionWarning(eventId = "mobile-config", reason = message)),
    )
}

private sealed interface MobileConfigArg {
    data object Default : MobileConfigArg
    data object Invalid : MobileConfigArg
    data class Path(val value: String) : MobileConfigArg
}
