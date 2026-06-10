package com.yonatankarp.agentdesk.design.theme

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ThemeModeTest :
    FunSpec({
        test("explicit modes ignore the system flag") {
            ThemeMode.Light.resolvesToDark(systemInDark = true) shouldBe false
            ThemeMode.Dark.resolvesToDark(systemInDark = false) shouldBe true
        }

        test("system mode follows the system flag") {
            ThemeMode.System.resolvesToDark(systemInDark = true) shouldBe true
            ThemeMode.System.resolvesToDark(systemInDark = false) shouldBe false
        }

        test("in-memory store round-trips and defaults to System") {
            val store = InMemoryThemeModeStore()
            store.load() shouldBe ThemeMode.System
            store.save(ThemeMode.Dark)
            store.load() shouldBe ThemeMode.Dark
        }

        test("next cycles System -> Light -> Dark -> System") {
            ThemeMode.System.next() shouldBe ThemeMode.Light
            ThemeMode.Light.next() shouldBe ThemeMode.Dark
            ThemeMode.Dark.next() shouldBe ThemeMode.System
        }
    })
