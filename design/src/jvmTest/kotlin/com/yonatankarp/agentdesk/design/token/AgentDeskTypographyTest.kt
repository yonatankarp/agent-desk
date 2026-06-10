package com.yonatankarp.agentdesk.design.token

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull

// Test-only opt-in, tracked debt #279.
@OptIn(ExperimentalTestApi::class)
class AgentDeskTypographyTest :
    FunSpec({
        test("typography families load inside a composition") {
            var typography: AgentDeskTypography? = null
            runComposeUiTest {
                setContent { typography = AgentDeskTypography() }
            }
            val loaded = typography.shouldNotBeNull()
            loaded.uiFamily.shouldNotBeNull()
            loaded.monoFamily.shouldNotBeNull()
        }
    })
