package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.app.operator.WorkItemInspector
import com.yonatankarp.agentdesk.app.runtime.summary
import com.yonatankarp.agentdesk.cli.input.CliCommand
import com.yonatankarp.agentdesk.cli.input.CliInputException
import com.yonatankarp.agentdesk.cli.input.CliOptions
import com.yonatankarp.agentdesk.cli.input.CliUsageException
import com.yonatankarp.agentdesk.cli.input.parseWorkItemId
import com.yonatankarp.agentdesk.cli.io.importMockRuntime
import com.yonatankarp.agentdesk.cli.io.importOpenClawObservations
import com.yonatankarp.agentdesk.cli.io.toOperatorState
import com.yonatankarp.agentdesk.cli.io.toWorkEventRead
import com.yonatankarp.agentdesk.cli.render.OperatorConsoleRenderer
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import java.io.InputStream
import java.io.PrintStream
import java.time.Instant
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
        now: () -> EventTimestamp = { EventTimestamp.parse(Instant.now().toString()) },
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
                state.storeReadWarning?.let { warning -> error.println("Warning: $warning") }
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
                output.println(result.diagnostics.summary().publicMessage())
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
                output.println(result.diagnostics.summary().publicMessage())
            }

            is CliCommand.Inspect -> {
                val workItemId = parseWorkItemId(command.rawWorkItemId)
                val read = options.toWorkEventRead(input)
                read.trailingCorruption?.let { corruption ->
                    error.println("Warning: ${corruption.publicSafeMessage()}")
                }
                val inspection = WorkItemInspector.inspect(read.events, workItemId)
                    ?: throw CliInputException("Work item was not found.")
                output.println(renderer.render(inspection))
            }

            is CliCommand.Act -> {
                val eventStorePath = command.eventStorePath
                    ?: throw CliUsageException("Missing value for --event-store.")
                val auditStorePath = command.auditStorePath
                    ?: throw CliUsageException("Missing value for --audit-store.")
                val result = ActCommand.execute(
                    intent = command.intent,
                    workItemId = parseWorkItemId(command.rawWorkItemId),
                    eventStorePath = eventStorePath,
                    auditStorePath = auditStorePath,
                    approve = command.approve,
                    now = now(),
                )
                output.println(result.text)
                if (result.exitCode != 0) {
                    return result.exitCode
                }
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

    private fun usage(): String =
        """
        Agent Desk

        Usage:
          agent-desk [--sample]
          agent-desk import-mock-runtime --event-store <file>
          agent-desk import-openclaw-observations --observations <file> --event-store <file>
          agent-desk act resume <work-item-id> --event-store <file> --audit-store <file> [--approve]
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
                          Route a public-safe mock resume through the permission gate and
                          approval loop, recording the decision and durable audit evidence.
                          Without --approve the gate denies and the denial is audited (exit 3).
          inspect        Render one sanitized work item by id.
          --event-store <file>
                          Local event store target for import commands or act.
          --audit-store <file>
                          Local audit store target for act decisions and outcomes.
          --approve       Approve the gated action explicitly. Only valid with act.
          --observations <file>
                          Sanitized observation export for import-openclaw-observations.
          --sample        Render built-in public-safe sample state.
          --events <file> Read newline-delimited sanitized work event JSON records.
          --stdin         Read newline-delimited sanitized work event JSON records from stdin.
          --config <file> Read public-safe runtime configuration properties.
          --help          Show this help.
        """.trimIndent()
}
