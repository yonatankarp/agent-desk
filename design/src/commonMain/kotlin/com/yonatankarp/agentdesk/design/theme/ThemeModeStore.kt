package com.yonatankarp.agentdesk.design.theme

interface ThemeModeStore {
    fun load(): ThemeMode
    fun save(mode: ThemeMode)
}

class InMemoryThemeModeStore(initial: ThemeMode = ThemeMode.System) : ThemeModeStore {
    private var current: ThemeMode = initial
    override fun load(): ThemeMode = current
    override fun save(mode: ThemeMode) {
        current = mode
    }
}
