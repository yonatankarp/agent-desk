package com.yonatankarp.agentdesk.design.token

import com.yonatankarp.agentdesk.design.resources.Res
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import kotlinx.coroutines.runBlocking

class FontResourceLoadsTest :
    FunSpec({
        test("bundled font resources actually load from the classpath") {
            runBlocking {
                Res.readBytes("font/inter_regular.ttf").size shouldBeGreaterThan 10_000
                Res.readBytes("font/jetbrainsmono_regular.ttf").size shouldBeGreaterThan 10_000
            }
        }
    })
