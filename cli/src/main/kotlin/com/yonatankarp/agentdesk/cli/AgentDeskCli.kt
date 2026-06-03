package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.SampleOperatorState
import com.yonatankarp.agentdesk.app.serialization.WorkEventJson
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.projections.WorkEventProjector
import java.io.IOException
import java.io.InputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
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
    }

    private fun CliOptions.toOperatorState(input: InputStream): OperatorState = when (mode) {
        CliInputMode.Sample -> SampleOperatorState.current()
        is CliInputMode.File -> readEventsFromFile(mode.path).toOperatorState()
        CliInputMode.Stdin -> readEventsFromInput(input).toOperatorState()
    }

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

    private fun List<WorkEvent>.toOperatorState(): OperatorState {
        val projection = WorkEventProjector.project(this)
        val issue = projection.ignoredEvents.firstOrNull()
        if (issue != null) {
            throw CliInputException("Invalid event sequence: ${issue.reason}.")
        }

        return OperatorState(
            workItems = projection.workItems,
            events = projection.recentEvents,
        )
    }

    private fun usage(): String =
        """
        Agent Desk

        Usage:
          agent-desk [--sample]
          agent-desk --events <file>
          agent-desk --stdin

        Options:
          --sample        Render built-in public-safe sample state.
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
}

private class CliInputException(
    val publicMessage: String,
) : RuntimeException(publicMessage)

private class CliUsageException(
    val publicMessage: String,
) : RuntimeException(publicMessage)
