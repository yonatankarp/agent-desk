package com.yonatankarp.agentdesk.cli.input

import com.yonatankarp.agentdesk.cli.ActCommand
import com.yonatankarp.agentdesk.cli.io.CliEventReads
import com.yonatankarp.agentdesk.cli.render.OperatorConsoleRenderer

class ForbiddenCliLayeringFixture(
    val command: ActCommand,
    val reads: CliEventReads,
    val renderer: OperatorConsoleRenderer,
)
