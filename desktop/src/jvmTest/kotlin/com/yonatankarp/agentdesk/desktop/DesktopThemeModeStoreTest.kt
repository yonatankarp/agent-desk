package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.design.theme.ThemeMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files

class DesktopThemeModeStoreTest :
    FunSpec({
        test("defaults to System when no file exists") {
            val dir = Files.createTempDirectory("adtheme")
            DesktopThemeModeStore(dir.resolve("theme")).load() shouldBe ThemeMode.System
        }

        test("persists and reloads the chosen mode") {
            val dir = Files.createTempDirectory("adtheme")
            val file = dir.resolve("theme")
            DesktopThemeModeStore(file).save(ThemeMode.Dark)
            DesktopThemeModeStore(file).load() shouldBe ThemeMode.Dark
        }

        test("falls back to System on unreadable content") {
            val dir = Files.createTempDirectory("adtheme")
            val file = dir.resolve("theme")
            Files.writeString(file, "NONSENSE")
            DesktopThemeModeStore(file).load() shouldBe ThemeMode.System
        }
    })
