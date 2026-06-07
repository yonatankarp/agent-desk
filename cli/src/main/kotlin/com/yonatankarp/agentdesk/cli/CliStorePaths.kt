package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.app.config.ConfigValidationException
import com.yonatankarp.agentdesk.app.config.EventStoreLocation
import com.yonatankarp.agentdesk.cli.input.CliInputException
import java.nio.file.InvalidPathException
import java.nio.file.Path

/**
 * Shared store-path acceptance for commands that touch local stores: the same
 * public-safe location grammar and error mapping for act and report.
 */
internal fun parseStorePath(
    path: String,
    label: String,
): Path = try {
    Path.of(EventStoreLocation.parse(path).value)
} catch (exception: ConfigValidationException) {
    throw CliInputException("Invalid $label store location: ${exception.message}")
} catch (_: InvalidPathException) {
    throw CliInputException("Configured $label store could not be updated.")
}
