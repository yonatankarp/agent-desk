package com.yonatankarp.agentdesk.cli.io

import com.yonatankarp.agentdesk.app.config.ConfigValidationException
import com.yonatankarp.agentdesk.app.config.EventStoreLocation
import com.yonatankarp.agentdesk.app.persistence.LocalFileWorkEventRepository
import com.yonatankarp.agentdesk.app.runtime.MockRuntimeWorkEventSource
import com.yonatankarp.agentdesk.app.runtime.RuntimeWorkEventImportException
import com.yonatankarp.agentdesk.app.runtime.RuntimeWorkEventImporter
import com.yonatankarp.agentdesk.app.runtime.RuntimeWorkEventSources
import com.yonatankarp.agentdesk.cli.input.CliInputException
import java.nio.file.InvalidPathException
import java.nio.file.Path

internal fun importMockRuntime(path: String) = try {
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

internal fun importOpenClawObservations(
    observationsPath: String,
    eventStorePath: String,
) = try {
    val location = EventStoreLocation.parse(eventStorePath)
    RuntimeWorkEventImporter(
        source = RuntimeWorkEventSources.openClawObservationFile(Path.of(observationsPath)),
        repository = LocalFileWorkEventRepository(Path.of(location.value)),
    ).importEvents()
} catch (exception: ConfigValidationException) {
    throw CliInputException("Invalid event store location: ${exception.message}")
} catch (exception: InvalidPathException) {
    throw CliInputException("Sanitized observation export could not be imported.")
} catch (exception: SecurityException) {
    throw CliInputException("Sanitized observation export could not be imported.")
} catch (exception: RuntimeWorkEventImportException) {
    throw CliInputException(exception.message ?: "Runtime events could not be imported.")
}
