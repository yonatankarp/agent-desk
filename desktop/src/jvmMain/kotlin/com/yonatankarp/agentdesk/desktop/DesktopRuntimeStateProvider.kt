package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.app.config.AgentDeskRuntimeConfig
import com.yonatankarp.agentdesk.app.config.AgentDeskRuntimeConfigParser
import com.yonatankarp.agentdesk.app.config.ConfigValidationException
import com.yonatankarp.agentdesk.app.operator.RuntimeConfiguredOperatorStateLoader
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.Properties

object DesktopRuntimeStateProvider {
    fun load(args: Array<String>): DesktopScreenState = try {
        val config = args.toConfig() ?: return DesktopScreenState.Error("Usage: agent-desk-desktop [--config <properties-file>]")
        DesktopStateResolver(RuntimeConfiguredOperatorStateLoader::load).resolve(config)
    } catch (error: ConfigValidationException) {
        DesktopScreenState.Error(error.message ?: "Configured operator state could not be loaded.")
    }

    private fun Array<String>.toConfig(): AgentDeskRuntimeConfig? {
        if (isEmpty()) {
            return AgentDeskRuntimeConfig.defaults()
        }
        if (size != 2 || this[0] != "--config") {
            return null
        }

        return try {
            AgentDeskRuntimeConfigParser.parse(readProperties(this[1]))
        } catch (error: ConfigValidationException) {
            throw error
        } catch (error: IOException) {
            throw ConfigValidationException("config file could not be read")
        } catch (error: InvalidPathException) {
            throw ConfigValidationException("config file could not be read")
        } catch (error: SecurityException) {
            throw ConfigValidationException("config file could not be read")
        }
    }

    private fun readProperties(path: String): Map<String, String> {
        val properties = Properties()
        Files.newBufferedReader(Path.of(path)).use(properties::load)
        return properties.entries.associate { (key, value) -> key.toString() to value.toString() }
    }
}
