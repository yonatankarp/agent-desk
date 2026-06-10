package com.yonatankarp.agentdesk.design.token

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldNotBe

/**
 * Drift pin: light and dark schemes must be distinct instances so a future edit
 * cannot accidentally collapse them to one (which would silently kill dark mode).
 */
class AgentDeskColorsTest :
    FunSpec({
        test("light and dark backgrounds differ") {
            AgentDeskColors.Light.background shouldNotBe AgentDeskColors.Dark.background
        }

        test("panel and row are distinct from background in both schemes") {
            listOf(AgentDeskColors.Light, AgentDeskColors.Dark).forEach { scheme ->
                scheme.panel shouldNotBe scheme.background
                scheme.row shouldNotBe scheme.background
            }
        }
    })
