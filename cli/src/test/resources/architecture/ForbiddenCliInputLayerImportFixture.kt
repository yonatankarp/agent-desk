package com.yonatankarp.agentdesk.cli.input.fixture

import com.yonatankarp.agentdesk.cli.AgentDeskCli
import com.yonatankarp.agentdesk.cli.io.toOperatorState
import com.yonatankarp.agentdesk.cli.render.OperatorConsoleRenderer

class ForbiddenCliInputLayerImportFixture(
    val dispatcher: AgentDeskCli,
    val renderer: OperatorConsoleRenderer,
    val readOperatorState: Any = ::toOperatorState,
)
