package com.yonatankarp.agentdesk.cli.input

internal sealed interface CliInputMode {
    data object Sample : CliInputMode

    data object Stdin : CliInputMode

    data class File(val path: String) : CliInputMode

    data class Config(val path: String) : CliInputMode
}
