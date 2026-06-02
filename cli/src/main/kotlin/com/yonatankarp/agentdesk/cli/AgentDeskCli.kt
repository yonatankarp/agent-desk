package com.yonatankarp.agentdesk.cli

fun main() {
    val renderer = OperatorConsoleRenderer()

    println(renderer.render(SampleOperatorState.current()))
}
