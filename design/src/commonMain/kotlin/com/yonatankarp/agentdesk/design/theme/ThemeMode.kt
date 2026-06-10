package com.yonatankarp.agentdesk.design.theme

enum class ThemeMode {
    System,
    Light,
    Dark,
    ;

    fun resolvesToDark(systemInDark: Boolean): Boolean = when (this) {
        System -> systemInDark
        Light -> false
        Dark -> true
    }

    fun next(): ThemeMode = when (this) {
        System -> Light
        Light -> Dark
        Dark -> System
    }
}
