package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.app.config.AgentDeskRuntimeConfig
import com.yonatankarp.agentdesk.app.config.AgentDeskRuntimeConfigParser
import com.yonatankarp.agentdesk.app.config.ConfigValidationException
import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.OperatorStateLoadException
import com.yonatankarp.agentdesk.app.operator.OperatorStateProjectionException
import com.yonatankarp.agentdesk.app.operator.OperatorStateProjector
import com.yonatankarp.agentdesk.app.operator.RuntimeConfiguredOperatorStateLoader
import com.yonatankarp.agentdesk.app.operator.SampleOperatorState
import com.yonatankarp.agentdesk.app.serialization.WorkEventJson
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import java.io.IOException
import java.io.InputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.Properties
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val exitCode = AgentDeskCli.run(args)
    if (exitCode != 0) {
        exitProcess(exitCode)
    }
}

object AgentDeskCli {
    fun run(
        args: Array<String>,
        input: InputStream = System.`in`,
        output: PrintStream = System.out,
        error: PrintStream = System.err,
    ): Int = try {
        val options = CliOptions.parse(args.toList())
        if (options.showHelp) {
            output.println(usage())
            return 0
        }

        val state = options.toOperatorState(input)
        output.println(OperatorConsoleRenderer().render(state))
        0
    } catch (exception: CliUsageException) {
        error.println("Error: ${exception.publicMessage}")
        error.println("Run with --help for usage.")
        2
    } catch (exception: CliInputException) {
        error.println("Error: ${exception.publicMessage}")
        1
    } catch (exception: ConfigValidationException) {
        error.println("Error: ${exception.message ?: "Runtime configuration is invalid."}")
        1
    } catch (exception: OperatorStateLoadException) {
        error.println("Error: ${exception.message ?: "Operator state could not be loaded."}")
        1
    } catch (exception: OperatorStateProjectionException) {
        error.println("Error: ${exception.message ?: "Operator state could not be projected."}")
        1
    }

    private fun CliOptions.toOperatorState(input: InputStream): OperatorState = when (mode) {
        CliInputMode.Sample -> SampleOperatorState.current()
        is CliInputMode.File -> readEventsFromFile(mode.path).toOperatorState()
        CliInputMode.Stdin -> readEventsFromInput(input).toOperatorState()
        is CliInputMode.Config -> loadRuntimeStateFromConfig(mode.path)
    }

    private fun loadRuntimeStateFromConfig(path: String): OperatorState {
        val config = readRuntimeConfig(path)
        return RuntimeConfiguredOperatorStateLoader().load(config)
    }

    private fun readRuntimeConfig(path: String): AgentDeskRuntimeConfig {
        val properties = Properties()
        try {
            Files.newInputStream(Path.of(path)).use(properties::load)
        } catch (exception: IOException) {
            throw CliInputException("Runtime configuration file could not be read.")
        } catch (exception: InvalidPathException) {
            throw CliInputException("Runtime configuration file could not be read.")
        } catch (exception: SecurityException) {
            throw CliInputException("Runtime configuration file could not be read.")
        } catch (exception: IllegalArgumentException) {
            throw CliInputException("Runtime configuration file could not be parsed.")
        }

        return AgentDeskRuntimeConfigParser.parse(properties.toStringMap())
    }

    private fun Properties.toStringMap(): Map<String, String> = stringPropertyNames().associateWith(::getProperty)

    private fun readEventsFromInput(input: InputStream): List<WorkEvent> = try {
        readEvents(input.readBytes().decodeToString())
    } catch (exception: IOException) {
        throw CliInputException("Event input could not be read.")
    }

    private fun readEventsFromFile(path: String): List<WorkEvent> {
        val raw =
            try {
                Files.readString(Path.of(path))
            } catch (exception: IOException) {
                throw CliInputException("Event input file could not be read.")
            } catch (exception: InvalidPathException) {
                throw CliInputException("Event input file could not be read.")
            } catch (exception: SecurityException) {
                throw CliInputException("Event input file could not be read.")
            }

        return readEvents(raw)
    }

    private fun readEvents(raw: String): List<WorkEvent> {
        val records = raw.lineSequence().mapIndexedNotNull { index, line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                null
            } else {
                index + 1 to trimmed
            }
        }.toList()

        if (records.isEmpty()) {
            throw CliInputException("No event input provided.")
        }

        val seenIds = mutableSetOf<String>()
        return records.map { (lineNumber, record) ->
            val event = decodeRecord(lineNumber, record)
            if (!seenIds.add(event.id.toString())) {
                throw CliInputException("Duplicate work event id at line $lineNumber.")
            }
            event
        }
    }

    private fun decodeRecord(
        lineNumber: Int,
        record: String,
    ): WorkEvent = try {
        WorkEventJson.decode(record)
    } catch (exception: IllegalArgumentException) {
        val message =
            if (exception.message.orEmpty().contains("Unknown work event type")) {
                "Unsupported event type at line $lineNumber."
            } else {
                "Invalid event record at line $lineNumber."
            }
        throw CliInputException(message)
    } catch (exception: RuntimeException) {
        throw CliInputException("Invalid event record at line $lineNumber.")
    }

    private fun List<WorkEvent>.toOperatorState(): OperatorState = OperatorStateProjector.project(this)

    private fun usage(): String =
        """
        Agent Desk

        Usage:
          agent-desk [--sample]
          agent-desk --config <file>
          agent-desk --events <file>
          agent-desk --stdin

        Options:
          --sample        Render built-in public-safe sample state.
          --config <file> Read public-safe runtime configuration properties.
          --events <file> Read newline-delimited sanitized work event JSON records.
          --stdin         Read newline-delimited sanitized work event JSON records from stdin.
          --help          Show this help.
        """.trimIndent()
}

private data class CliOptions(
    val mode: CliInputMode,
    val showHelp: Boolean = false,
) {
    companion object {
        fun parse(args: List<String>): CliOptions {
            if (args.isEmpty()) {
                return CliOptions(mode = CliInputMode.Sample)
            }

            var mode: CliInputMode? = null
            var showHelp = false
            var index = 0
            while (index < args.size) {
                when (val arg = args[index]) {
                    "--help", "-h" -> showHelp = true

                    "--sample" -> mode = mode.assign(CliInputMode.Sample)

                    "--stdin" -> mode = mode.assign(CliInputMode.Stdin)

                    "--config" -> {
                        val path = args.getOrNull(index + 1)
                            ?: throw CliUsageException("Missing value for --config.")
                        if (path.startsWith("-")) {
                            throw CliUsageException("Missing value for --config.")
                        }
                        mode = mode.assign(CliInputMode.Config(path))
                        index += 1
                    }

                    "--events" -> {
                        val path = args.getOrNull(index + 1)
                            ?: throw CliUsageException("Missing value for --events.")
                        if (path.startsWith("-")) {
                            throw CliUsageException("Missing value for --events.")
                        }
                        mode = mode.assign(CliInputMode.File(path))
                        index += 1
                    }

                    else -> throw CliUsageException("Unknown option.")
                }
                index += 1
            }

            return CliOptions(
                mode = mode ?: CliInputMode.Sample,
                showHelp = showHelp,
            )
        }

        private fun CliInputMode?.assign(next: CliInputMode): CliInputMode {
            if (this != null && this != next) {
                throw CliUsageException("Choose only one input mode.")
            }
            return next
        }
    }
}

private sealed interface CliInputMode {
    data object Sample : CliInputMode

    data object Stdin : CliInputMode

    data class File(val path: String) : CliInputMode

    data class Config(val path: String) : CliInputMode
}

private class CliInputException(
    val publicMessage: String,
) : RuntimeException(publicMessage)

private class CliUsageException(
    val publicMessage: String,
) : RuntimeException(publicMessage)
