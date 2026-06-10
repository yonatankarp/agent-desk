package com.yonatankarp.agentdesk.design.token

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull

// Test-only opt-in, tracked debt #279.
@OptIn(ExperimentalTestApi::class)
class AgentDeskTypographyTest :
    FunSpec({
        test("typography families load inside a composition") {
            runComposeUiTest {
                setContent {
                    rememberedTypography = AgentDeskTypography()
                }
            }
            rememberedTypography.shouldNotBeNull()
            rememberedTypography!!.uiFamily.shouldNotBeNull()
            rememberedTypography!!.monoFamily.shouldNotBeNull()
        }
    })

private var rememberedTypography: AgentDeskTypography? = null
