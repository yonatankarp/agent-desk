package com.yonatankarp.agentdesk.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WorkItemIdTest {
    @Test
    fun normalizesWhitespaceAndCase() {
        val id = WorkItemId.parse("  Agent-Task:42  ")

        assertEquals("agent-task:42", id.value)
        assertEquals("agent-task:42", id.toString())
    }

    @Test
    fun rejectsBlankIds() {
        assertFailsWith<IllegalArgumentException> {
            WorkItemId.parse("   ")
        }
    }

    @Test
    fun rejectsUnsupportedCharacters() {
        assertFailsWith<IllegalArgumentException> {
            WorkItemId.parse("agent task")
        }
    }
}
