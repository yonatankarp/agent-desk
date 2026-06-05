package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.app.config.AgentDeskRuntimeConfigParser
import com.yonatankarp.agentdesk.app.config.ConfigValidationException
import com.yonatankarp.agentdesk.app.config.EventStoreLocation
import com.yonatankarp.agentdesk.app.operator.MockOperatorActionAdapter
import com.yonatankarp.agentdesk.app.operator.OperatorActionException
import com.yonatankarp.agentdesk.app.operator.OperatorActionIntent
import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.OperatorStateProjectionException
import com.yonatankarp.agentdesk.app.operator.OperatorStateProjector
import com.yonatankarp.agentdesk.app.operator.RuntimeConfiguredOperatorStateLoadException
import com.yonatankarp.agentdesk.app.operator.RuntimeConfiguredOperatorStateLoader
import com.yonatankarp.agentdesk.app.operator.RuntimeConfiguredWorkEventLoader
import com.yonatankarp.agentdesk.app.operator.SampleOperatorState
import com.yonatankarp.agentdesk.app.operator.WorkItemInspector
import com.yonatankarp.agentdesk.app.persistence.LocalFileWorkEventRepository
import com.yonatankarp.agentdesk.app.persistence.WorkEventStoreException
import com.yonatankarp.agentdesk.app.runtime.MockRuntimeWorkEventSource
import com.yonatankarp.agentdesk.app.runtime.OpenClawRuntimeObservationFileSource
import com.yonatankarp.agentdesk.app.runtime.OpenClawRuntimeObservationFileSourceException
import com.yonatankarp.agentdesk.app.runtime.RuntimeWorkEventImportException
import com.yonatankarp.agentdesk.app.runtime.RuntimeWorkEventImporter
import com.yonatankarp.agentdesk.app.serialization.WorkEventJson
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
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

        val renderer = OperatorConsoleRenderer()
        when (val command = options.command) {
            CliCommand.Dashboard -> {
                val state = options.toOperatorState(input)
                output.println(renderer.render(state))
            }

            is CliCommand.ImportMockRuntime -> {
                val eventStorePath = command.eventStorePath
                    ?: throw CliUsageException("Missing value for --event-store.")
                val result = importMockRuntime(eventStorePath)
                output.println(
                    "Imported ${result.importedCount} mock runtime event(s); " +
                        "skipped ${result.skippedDuplicateCount} duplicate event(s).",
                )
            }

            is CliCommand.ImportOpenClawObservations -> {
                val observationsPath = command.observationsPath
                    ?: throw CliUsageException("Missing value for --observations.")
                val eventStorePath = command.eventStorePath
                    ?: throw CliUsageException("Missing value for --event-store.")
                val result = importOpenClawObservations(
                    observationsPath = observationsPath,
                    eventStorePath = eventStorePath,
                )
                output.println(
                    "Imported ${result.importedCount} sanitized observation event(s); " +
                        "skipped ${result.skippedDuplicateCount} duplicate event(s).",
                )
            }

            is CliCommand.Inspect -> {
                val workItemId = parseWorkItemId(command.rawWorkItemId)
                val events = options.toWorkEvents(input)
                val inspection = WorkItemInspector.inspect(events, workItemId)
                    ?: throw CliInputException("Work item was not found.")
                output.println(renderer.render(inspection))
            }

            is CliCommand.Act -> {
                val eventStorePath = command.eventStorePath
                    ?: throw CliUsageException("Missing value for --event-store.")
                val event = performMockAction(command, eventStorePath)
                output.println(
                    "Recorded ${command.intent.wireName} action for ${event.workItemId} " +
                        "as ${event.id}.",
                )
            }
        }
        0
    } catch (exception: CliUsageException) {
        error.println("Error: ${exception.publicMessage}")
        error.println("Run with --help for usage.")
        2
    } catch (exception: CliInputException) {
        error.println("Error: ${exception.publicMessage}")
        1
    }

    private fun importMockRuntime(path: String) = try {
        val location = EventStoreLocation.parse(path)
        RuntimeWorkEventImporter(
            source = MockRuntimeWorkEventSource(),
            repository = LocalFileWorkEventRepository(Path.of(location.value)),
        ).importEvents()
    } catch (exception: ConfigValidationException) {
        throw CliInputException("Invalid event store location: ${exception.message}")
    } catch (exception: InvalidPathException) {
        throw CliInputException("Configured event store could not be written.")
    } catch (exception: SecurityException) {
        throw CliInputException("Configured event store could not be written.")
    } catch (exception: RuntimeWorkEventImportException) {
        throw CliInputException(exception.message ?: "Runtime events could not be imported.")
    }

    private fun importOpenClawObservations(
        observationsPath: String,
        eventStorePath: String,
    ) = try {
        val location = EventStoreLocation.parse(eventStorePath)
        RuntimeWorkEventImporter(
            source = OpenClawRuntimeObservationFileSource(Path.of(observationsPath)),
            repository = LocalFileWorkEventRepository(Path.of(location.value)),
        ).importEvents()
    } catch (exception: ConfigValidationException) {
        throw CliInputException("Invalid event store location: ${exception.message}")
    } catch (exception: InvalidPathException) {
        throw CliInputException("Sanitized observation export could not be imported.")
    } catch (exception: SecurityException) {
        throw CliInputException("Sanitized observation export could not be imported.")
    } catch (exception: OpenClawRuntimeObservationFileSourceException) {
        throw CliInputException(exception.message ?: "Sanitized observation export could not be imported.")
    } catch (exception: RuntimeWorkEventImportException) {
        throw CliInputException(exception.message ?: "Runtime events could not be imported.")
    }

    private fun performMockAction(
        command: CliCommand.Act,
        path: String,
    ): WorkEvent = try {
        val location = EventStoreLocation.parse(path)
        val repository = LocalFileWorkEventRepository(Path.of(location.value))
        val event = MockOperatorActionAdapter().perform(
            intent = command.intent,
            workItemId = parseWorkItemId(command.rawWorkItemId),
            events = repository.readAll(),
        )
        repository.append(event)
        event
    } catch (exception: ConfigValidationException) {
        throw CliInputException("Invalid event store location: ${exception.message}")
    } catch (exception: InvalidPathException) {
        throw CliInputException("Configured event store could not be updated.")
    } catch (exception: SecurityException) {
        throw CliInputException("Configured event store could not be updated.")
    } catch (exception: WorkEventStoreException) {
        throw CliInputException(exception.message ?: "Configured event store could not be updated.")
    } catch (exception: OperatorActionException) {
        throw CliInputException(exception.message ?: "Mock operator action could not be applied.")
    } catch (exception: OperatorStateProjectionException) {
        throw CliInputException(exception.message ?: "Mock operator action could not read operator state.")
    }

    private fun CliOptions.toOperatorState(input: InputStream): OperatorState = when (mode) {
        CliInputMode.Sample -> SampleOperatorState.current()
        is CliInputMode.File -> readEventsFromFile(mode.path).toOperatorState()
        CliInputMode.Stdin -> readEventsFromInput(input).toOperatorState()
        is CliInputMode.Config -> readConfiguredState(mode.path)
    }

    private fun CliOptions.toWorkEvents(input: InputStream): List<WorkEvent> = when (mode) {
        CliInputMode.Sample -> SampleOperatorState.current().events
        is CliInputMode.File -> readEventsFromFile(mode.path)
        CliInputMode.Stdin -> readEventsFromInput(input)
        is CliInputMode.Config -> readConfiguredEvents(mode.path)
    }

    private fun readConfiguredState(path: String): OperatorState {
        val values = readConfig(path)
        val config =
            try {
                AgentDeskRuntimeConfigParser.parse(values)
            } catch (exception: ConfigValidationException) {
                throw CliInputException("Invalid runtime config: ${exception.message}")
            }

        return try {
            RuntimeConfiguredOperatorStateLoader.load(config)
        } catch (exception: RuntimeConfiguredOperatorStateLoadException) {
            throw CliInputException(exception.message ?: "Configured runtime state could not be loaded.")
        }
    }

    private fun readConfiguredEvents(path: String): List<WorkEvent> {
        val values = readConfig(path)
        val config =
            try {
                AgentDeskRuntimeConfigParser.parse(values)
            } catch (exception: ConfigValidationException) {
                throw CliInputException("Invalid runtime config: ${exception.message}")
            }

        return try {
            RuntimeConfiguredWorkEventLoader.load(config)
        } catch (exception: RuntimeConfiguredOperatorStateLoadException) {
            throw CliInputException(exception.message ?: "Configured runtime state could not be loaded.")
        }
    }

    private fun parseWorkItemId(raw: String): WorkItemId = try {
        WorkItemId.parse(raw)
    } catch (exception: IllegalArgumentException) {
        throw CliUsageException("Invalid work item id.")
    }

    private fun readConfig(path: String): Map<String, String> {
        val properties = Properties()
        try {
            Files.newInputStream(Path.of(path)).use(properties::load)
        } catch (exception: IOException) {
            throw CliInputException("Runtime config file could not be read.")
        } catch (exception: InvalidPathException) {
            throw CliInputException("Runtime config file could not be read.")
        } catch (exception: IllegalArgumentException) {
            throw CliInputException("Runtime config file could not be read.")
        } catch (exception: SecurityException) {
            throw CliInputException("Runtime config file could not be read.")
        }

        return properties.stringPropertyNames().associateWith(properties::getProperty)
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

    private fun List<WorkEvent>.toOperatorState(): OperatorState = try {
        OperatorStateProjector.project(this)
    } catch (exception: OperatorStateProjectionException) {
        throw CliInputException(exception.message ?: "Invalid event sequence.")
    }

    private fun usage(): String =
        """
        Agent Desk

        Usage:
          agent-desk [--sample]
          agent-desk import-mock-runtime --event-store <file>
          agent-desk import-openclaw-observations --observations <file> --event-store <file>
          agent-desk act resume <work-item-id> --event-store <file>
          agent-desk inspect <work-item-id> [--sample]
          agent-desk inspect <work-item-id> --events <file>
          agent-desk inspect <work-item-id> --stdin
          agent-desk inspect <work-item-id> --config <file>
          agent-desk --events <file>
          agent-desk --stdin
          agent-desk --config <file>

        Options:
          import-mock-runtime
                          Import public-safe mock runtime events into a local event store.
          import-openclaw-observations
                          Import a sanitized observation export into a local event store.
          act resume <work-item-id>
                          Append a public-safe mock resume action event to a local event store.
          inspect        Render one sanitized work item by id.
          --event-store <file>
                          Local event store target for import commands or act.
          --observations <file>
                          Sanitized observation export for import-openclaw-observations.
          --sample        Render built-in public-safe sample state.
          --events <file> Read newline-delimited sanitized work event JSON records.
          --stdin         Read newline-delimited sanitized work event JSON records from stdin.
          --config <file> Read public-safe runtime configuration properties.
          --help          Show this help.
        """.trimIndent()
}

private data class CliOptions(
    val mode: CliInputMode,
    val command: CliCommand = CliCommand.Dashboard,
    val showHelp: Boolean = false,
) {
    companion object {
        fun parse(args: List<String>): CliOptions {
            if (args.isEmpty()) {
                return CliOptions(mode = CliInputMode.Sample)
            }

            var mode: CliInputMode? = null
            var command: CliCommand = CliCommand.Dashboard
            var showHelp = false
            var index = 0
            while (index < args.size) {
                when (val arg = args[index]) {
                    "--help", "-h" -> showHelp = true

                    "inspect" -> {
                        if (command != CliCommand.Dashboard) {
                            throw CliUsageException("Choose only one command.")
                        }
                        val workItemId = args.getOrNull(index + 1)
                            ?: throw CliUsageException("Missing work item id for inspect.")
                        if (workItemId.startsWith("-")) {
                            throw CliUsageException("Missing work item id for inspect.")
                        }
                        command = CliCommand.Inspect(workItemId)
                        index += 1
                    }

                    "import-mock-runtime" -> {
                        if (command != CliCommand.Dashboard) {
                            throw CliUsageException("Choose only one command.")
                        }
                        command = CliCommand.ImportMockRuntime()
                    }

                    "import-openclaw-observations" -> {
                        if (command != CliCommand.Dashboard) {
                            throw CliUsageException("Choose only one command.")
                        }
                        command = CliCommand.ImportOpenClawObservations()
                    }

                    "act" -> {
                        if (command != CliCommand.Dashboard) {
                            throw CliUsageException("Choose only one command.")
                        }
                        val rawIntent = args.getOrNull(index + 1)
                            ?: throw CliUsageException("Missing action intent for act.")
                        val workItemId = args.getOrNull(index + 2)
                            ?: throw CliUsageException("Missing work item id for act.")
                        if (rawIntent.startsWith("-")) {
                            throw CliUsageException("Missing action intent for act.")
                        }
                        if (workItemId.startsWith("-")) {
                            throw CliUsageException("Missing work item id for act.")
                        }
                        command = CliCommand.Act(
                            intent = parseActionIntent(rawIntent),
                            rawWorkItemId = workItemId,
                        )
                        index += 2
                    }

                    "--event-store" -> {
                        val path = args.getOrNull(index + 1)
                            ?: throw CliUsageException("Missing value for --event-store.")
                        if (path.startsWith("-")) {
                            throw CliUsageException("Missing value for --event-store.")
                        }
                        command = when (val selectedCommand = command) {
                            is CliCommand.ImportMockRuntime -> {
                                if (selectedCommand.eventStorePath != null) {
                                    throw CliUsageException("Choose only one event store.")
                                }
                                selectedCommand.copy(eventStorePath = path)
                            }

                            is CliCommand.Act -> {
                                if (selectedCommand.eventStorePath != null) {
                                    throw CliUsageException("Choose only one event store.")
                                }
                                selectedCommand.copy(eventStorePath = path)
                            }

                            is CliCommand.ImportOpenClawObservations -> {
                                if (selectedCommand.eventStorePath != null) {
                                    throw CliUsageException("Choose only one event store.")
                                }
                                selectedCommand.copy(eventStorePath = path)
                            }

                            else -> throw CliUsageException(
                                "--event-store is only valid with import commands or act.",
                            )
                        }
                        index += 1
                    }

                    "--observations" -> {
                        val path = args.getOrNull(index + 1)
                            ?: throw CliUsageException("Missing value for --observations.")
                        if (path.startsWith("-")) {
                            throw CliUsageException("Missing value for --observations.")
                        }
                        command = when (val selectedCommand = command) {
                            is CliCommand.ImportOpenClawObservations -> {
                                if (selectedCommand.observationsPath != null) {
                                    throw CliUsageException("Choose only one observations export.")
                                }
                                selectedCommand.copy(observationsPath = path)
                            }

                            else -> throw CliUsageException(
                                "--observations is only valid with import-openclaw-observations.",
                            )
                        }
                        index += 1
                    }

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

                    "--config" -> {
                        val path = args.getOrNull(index + 1)
                            ?: throw CliUsageException("Missing value for --config.")
                        if (path.startsWith("-")) {
                            throw CliUsageException("Missing value for --config.")
                        }
                        mode = mode.assign(CliInputMode.Config(path))
                        index += 1
                    }

                    else -> throw CliUsageException("Unknown option.")
                }
                index += 1
            }

            if (command is CliCommand.ImportMockRuntime && command.eventStorePath == null) {
                throw CliUsageException("Missing value for --event-store.")
            }
            if (command is CliCommand.ImportOpenClawObservations && command.observationsPath == null) {
                throw CliUsageException("Missing value for --observations.")
            }
            if (command is CliCommand.ImportOpenClawObservations && command.eventStorePath == null) {
                throw CliUsageException("Missing value for --event-store.")
            }
            if (command is CliCommand.Act && command.eventStorePath == null) {
                throw CliUsageException("Missing value for --event-store.")
            }
            if (
                (
                    command is CliCommand.ImportMockRuntime ||
                        command is CliCommand.ImportOpenClawObservations ||
                        command is CliCommand.Act
                    ) &&
                mode != null
            ) {
                throw CliUsageException("Choose only one input mode.")
            }

            return CliOptions(
                mode = mode ?: CliInputMode.Sample,
                command = command,
                showHelp = showHelp,
            )
        }

        private fun CliInputMode?.assign(next: CliInputMode): CliInputMode {
            if (this != null && this != next) {
                throw CliUsageException("Choose only one input mode.")
            }
            return next
        }

        private fun parseActionIntent(raw: String): OperatorActionIntent = try {
            OperatorActionIntent.fromWireName(raw)
        } catch (exception: OperatorActionException) {
            throw CliUsageException(exception.message ?: "Unsupported operator action.")
        }
    }
}

private sealed interface CliCommand {
    data object Dashboard : CliCommand

    data class Inspect(val rawWorkItemId: String) : CliCommand

    data class ImportMockRuntime(val eventStorePath: String? = null) : CliCommand

    data class ImportOpenClawObservations(
        val observationsPath: String? = null,
        val eventStorePath: String? = null,
    ) : CliCommand

    data class Act(
        val intent: OperatorActionIntent,
        val rawWorkItemId: String,
        val eventStorePath: String? = null,
    ) : CliCommand
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
