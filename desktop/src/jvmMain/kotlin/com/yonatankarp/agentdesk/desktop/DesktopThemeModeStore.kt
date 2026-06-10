package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.design.theme.ThemeMode
import com.yonatankarp.agentdesk.design.theme.ThemeModeStore
import java.nio.file.Files
import java.nio.file.Path

class DesktopThemeModeStore(private val file: Path) : ThemeModeStore {
    override fun load(): ThemeMode = runCatching {
        ThemeMode.valueOf(Files.readString(file).trim())
    }.getOrDefault(ThemeMode.System)

    override fun save(mode: ThemeMode) {
        runCatching {
            Files.createDirectories(file.parent)
            Files.writeString(file, mode.name)
        }
    }
}
