package com.yonatankarp.agentdesk.cli.io

import com.yonatankarp.agentdesk.app.config.AgentDeskRuntimeConfigParser
import com.yonatankarp.agentdesk.app.config.ConfigValidationException
import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.OperatorStateProjectionException
import com.yonatankarp.agentdesk.app.operator.OperatorStateProjector
import com.yonatankarp.agentdesk.app.operator.RuntimeConfiguredOperatorStateLoadException
import com.yonatankarp.agentdesk.app.operator.RuntimeConfiguredOperatorStateLoader
import com.yonatankarp.agentdesk.app.operator.RuntimeConfiguredWorkEventLoader
import com.yonatankarp.agentdesk.app.operator.SampleOperatorState
import com.yonatankarp.agentdesk.app.persistence.WorkEventReadResult
import com.yonatankarp.agentdesk.app.serialization.WorkEventJson
import com.yonatankarp.agentdesk.cli.input.CliInputException
import com.yonatankarp.agentdesk.cli.input.CliInputMode
import com.yonatankarp.agentdesk.cli.input.CliOptions
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.Properties

internal fun CliOptions.toOperatorState(input: InputStream): OperatorState = when (mode) {
    CliInputMode.Sample -> SampleOperatorState.current()
    is CliInputMode.File -> readEventsFromFile(mode.path).toOperatorState()
    CliInputMode.Stdin -> readEventsFromInput(input).toOperatorState()
    is CliInputMode.Config -> readConfiguredState(mode.path)
}

internal fun CliOptions.toWorkEventRead(input: InputStream): WorkEventReadResult = when (mode) {
    CliInputMode.Sample -> WorkEventReadResult(events = SampleOperatorState.current().events)
    is CliInputMode.File -> WorkEventReadResult(events = readEventsFromFile(mode.path))
    CliInputMode.Stdin -> WorkEventReadResult(events = readEventsFromInput(input))
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

private fun readConfiguredEvents(path: String): WorkEventReadResult {
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
