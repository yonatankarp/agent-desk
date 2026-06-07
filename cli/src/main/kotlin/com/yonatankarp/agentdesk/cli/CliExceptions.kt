package com.yonatankarp.agentdesk.cli

internal class CliInputException(
    val publicMessage: String,
) : RuntimeException(publicMessage)

internal class CliUsageException(
    val publicMessage: String,
) : RuntimeException(publicMessage)
