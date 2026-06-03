package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.app.operator.SampleOperatorState

fun main() {
    val renderer = OperatorConsoleRenderer()

    println(renderer.render(SampleOperatorState.current()))
}
