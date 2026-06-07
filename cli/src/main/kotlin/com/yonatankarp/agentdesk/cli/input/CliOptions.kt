package com.yonatankarp.agentdesk.cli.input

import com.yonatankarp.agentdesk.app.operator.OperatorActionException
import com.yonatankarp.agentdesk.app.operator.OperatorActionIntent
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId

internal data class CliOptions(
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
                when (args[index]) {
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

                    "report" -> {
                        if (command != CliCommand.Dashboard) {
                            throw CliUsageException("Choose only one command.")
                        }
                        val workItemId = args.getOrNull(index + 1)
                            ?: throw CliUsageException("Missing work item id for report.")
                        if (workItemId.startsWith("-")) {
                            throw CliUsageException("Missing work item id for report.")
                        }
                        command = CliCommand.Report(workItemId)
                        index += 1
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

                    "--audit-store" -> {
                        val path = args.getOrNull(index + 1)
                            ?: throw CliUsageException("Missing value for --audit-store.")
                        if (path.startsWith("-")) {
                            throw CliUsageException("Missing value for --audit-store.")
                        }
                        command = when (val selectedCommand = command) {
                            is CliCommand.Act -> {
                                if (selectedCommand.auditStorePath != null) {
                                    throw CliUsageException("Choose only one audit store.")
                                }
                                selectedCommand.copy(auditStorePath = path)
                            }

                            is CliCommand.Report -> {
                                if (selectedCommand.auditStorePath != null) {
                                    throw CliUsageException("Choose only one audit store.")
                                }
                                selectedCommand.copy(auditStorePath = path)
                            }

                            else -> throw CliUsageException("--audit-store is only valid with act or report.")
                        }
                        index += 1
                    }

                    "--approve" -> {
                        command = when (val selectedCommand = command) {
                            is CliCommand.Act -> selectedCommand.copy(approve = true)
                            else -> throw CliUsageException("--approve is only valid with act.")
                        }
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
            if (command is CliCommand.Act && command.auditStorePath == null) {
                throw CliUsageException("Missing value for --audit-store.")
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

internal fun parseWorkItemId(raw: String): WorkItemId = try {
    WorkItemId.parse(raw)
} catch (exception: IllegalArgumentException) {
    throw CliUsageException("Invalid work item id.")
}
