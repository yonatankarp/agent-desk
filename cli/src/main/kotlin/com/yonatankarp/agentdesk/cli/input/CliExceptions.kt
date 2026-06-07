package com.yonatankarp.agentdesk.cli.input

internal class CliInputException(
    val publicMessage: String,
) : RuntimeException(publicMessage)

internal class CliUsageException(
    val publicMessage: String,
) : RuntimeException(publicMessage)
